package com.ember.service;

import com.ember.config.EmberProperties;
import com.ember.web.dto.AssistantMessage;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.anthropic.models.messages.ToolUseBlockParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The admin AI assistant. It answers a manager's questions by calling read-only
 * "tools" backed by the reporting services, so every figure is grounded in the shop's
 * real data. Requests go to the LLM gateway configured in the admin panel
 * ({@link AssistantConfigService}); the key never leaves the server.
 */
@Service
public class AssistantService {

    private static final int MAX_TOOL_ROUNDS = 6;

    private final AssistantConfigService config;
    private final ReportService reports;
    private final MenuService menu;
    private final EmberProperties props;
    private final ObjectMapper json;

    public AssistantService(AssistantConfigService config, ReportService reports,
                            MenuService menu, EmberProperties props, ObjectMapper json) {
        this.config = config;
        this.reports = reports;
        this.menu = menu;
        this.props = props;
        this.json = json;
    }

    public String chat(List<AssistantMessage> history) {
        if (!config.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI assistant is not configured. Add a gateway API key in the admin panel.");
        }

        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(config.apiKey())
                .baseUrl(config.baseUrl())
                .build();

        List<MessageParam> messages = new ArrayList<>();
        for (AssistantMessage m : history) {
            MessageParam.Role role = "assistant".equals(m.role())
                    ? MessageParam.Role.ASSISTANT : MessageParam.Role.USER;
            messages.add(MessageParam.builder().role(role).content(m.content()).build());
        }

        try {
            for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                MessageCreateParams.Builder params = MessageCreateParams.builder()
                        .model(config.model())
                        .maxTokens(1500L)
                        .system(systemPrompt())
                        .messages(messages);
                tools().forEach(params::addTool);

                Message response = client.messages().create(params.build());

                List<ContentBlockParam> assistantBlocks = new ArrayList<>();
                List<ContentBlockParam> toolResults = new ArrayList<>();
                StringBuilder text = new StringBuilder();

                for (ContentBlock block : response.content()) {
                    if (block.text().isPresent()) {
                        String t = block.text().get().text();
                        text.append(t);
                        assistantBlocks.add(ContentBlockParam.ofText(
                                TextBlockParam.builder().text(t).build()));
                    } else if (block.toolUse().isPresent()) {
                        ToolUseBlock tu = block.toolUse().get();
                        assistantBlocks.add(ContentBlockParam.ofToolUse(ToolUseBlockParam.builder()
                                .id(tu.id()).name(tu.name()).input(tu._input()).build()));
                        String result = runTool(tu.name(), tu._input());
                        toolResults.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                                .toolUseId(tu.id()).content(result).build()));
                    }
                }

                messages.add(MessageParam.builder()
                        .role(MessageParam.Role.ASSISTANT)
                        .contentOfBlockParams(assistantBlocks)
                        .build());

                if (toolResults.isEmpty()) {
                    return text.length() > 0 ? text.toString()
                            : "I don't have an answer for that.";
                }
                messages.add(MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .contentOfBlockParams(toolResults)
                        .build());
            }
            return "I looked into that but couldn't finish — please try rephrasing.";
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The assistant gateway call failed: " + e.getMessage());
        }
    }

    /* ----- tools ----- */

    private List<Tool> tools() {
        return List.of(
                tool("get_sales_analytics",
                        "Net sales analytics for a date range (defaults to the last 7 days). Returns "
                        + "order count, revenue, average order value, sales by day, top items, sales by "
                        + "category, by order type, by hour, and per-staff sales.",
                        true),
                tool("get_staff_performance",
                        "Per-staff hours worked (from the time clock) and net sales for a date range "
                        + "(defaults to today). Returns hours, orders served, sales, and sales per hour.",
                        true),
                tool("get_low_stock",
                        "Menu items that are sold out or running low on tracked stock. No arguments.",
                        false),
                tool("get_menu",
                        "The full current menu: every item with its category, base price, and stock "
                        + "state. No arguments.",
                        false));
    }

    private static Tool tool(String name, String description, boolean dateRange) {
        Tool.InputSchema.Properties.Builder properties = Tool.InputSchema.Properties.builder();
        if (dateRange) {
            properties.putAdditionalProperty("from_date", JsonValue.from(
                    Map.of("type", "string", "description", "Start date, inclusive, as YYYY-MM-DD.")));
            properties.putAdditionalProperty("to_date", JsonValue.from(
                    Map.of("type", "string", "description", "End date, inclusive, as YYYY-MM-DD.")));
        }
        return Tool.builder()
                .name(name)
                .description(description)
                .inputSchema(Tool.InputSchema.builder().properties(properties.build()).build())
                .build();
    }

    private String runTool(String name, JsonValue input) {
        try {
            LocalDate today = LocalDate.now(props.getTimezone());
            Map<String, Object> args = argsOf(input);
            return switch (name) {
                case "get_sales_analytics" -> write(reports.analytics(
                        date(args, "from_date", today.minusDays(6)), date(args, "to_date", today)));
                case "get_staff_performance" -> write(reports.labor(
                        date(args, "from_date", today), date(args, "to_date", today)));
                case "get_low_stock" -> write(reports.lowStock());
                case "get_menu" -> write(menu.list());
                default -> "{\"error\":\"unknown tool\"}";
            };
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private Map<String, Object> argsOf(JsonValue input) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = json.convertValue(input, Map.class);
            return map != null ? map : Map.of();
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    private static LocalDate date(Map<String, Object> args, String key, LocalDate fallback) {
        Object v = args.get(key);
        if (v == null || v.toString().isBlank()) return fallback;
        try {
            return LocalDate.parse(v.toString().trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private String write(Object value) throws Exception {
        return json.writeValueAsString(value);
    }

    private String systemPrompt() {
        LocalDate today = LocalDate.now(props.getTimezone());
        return """
                You are the Ember assistant, a data analyst embedded in the admin panel of Ember, \
                a quick-service (fast-food) restaurant management system. A manager of the shop is \
                asking you questions. Use the provided tools to fetch real, live data from THIS shop \
                before answering — never invent or estimate numbers. Today's date is %s (timezone %s). \
                All money is in US dollars. Be concise and specific; every figure you state must come \
                from a tool result. If a question is not about the shop's data or operations, answer \
                briefly and say it's outside what you can help with here.""".formatted(today, props.getTimezone());
    }
}
