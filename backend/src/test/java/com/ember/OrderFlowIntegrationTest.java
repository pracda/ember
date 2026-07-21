package com.ember;

import com.ember.web.dto.OrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end happy path over the real HTTP + WebSocket stack: the POS creates an
 * order, the kitchen advances it to READY, the board collects it — and every
 * step is observed as a STOMP broadcast on {@code /topic/orders}, the single
 * stream all three stations share.
 *
 * <p>Origins are opened to {@code *} for the test client so the SockJS handshake
 * is not rejected.</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ember.allowed-origins=*")
class OrderFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private final BlockingQueue<Map<String, Object>> events = new LinkedBlockingQueue<>();

    @SuppressWarnings("unchecked")
    private StompSession subscribe() throws Exception {
        var stomp = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stomp.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession session = stomp
                .connectAsync("http://localhost:" + port + "/ws", new StompSessionHandlerAdapter() { })
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/orders", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class; // decode as a generic map; avoids Instant deserialization
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                events.add((Map<String, Object>) payload);
            }
        });
        return session;
    }

    private String nextEventType() throws InterruptedException {
        Map<String, Object> evt = events.poll(5, TimeUnit.SECONDS);
        assertThat(evt).as("expected a broadcast within 5s").isNotNull();
        return (String) evt.get("type");
    }

    private long postForId(String path) {
        ResponseEntity<OrderResponse> resp =
                rest.postForEntity("/api/orders" + path, null, OrderResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().id();
    }

    @Test
    void createAdvanceReadyCollectBroadcastsEachStep() throws Exception {
        StompSession session = subscribe();
        try {
            String createBody = """
                    {"type":"DINE_IN","lines":[
                      {"itemId":"b1","quantity":1,"meal":true,"addons":["No onion","Extra cheese"],"notes":"well done"},
                      {"itemId":"d1","quantity":1,"size":"Large"}
                    ]}""";

            var headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            ResponseEntity<OrderResponse> created = rest.postForEntity(
                    "/api/orders",
                    new org.springframework.http.HttpEntity<>(createBody, headers),
                    OrderResponse.class);

            assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            OrderResponse order = created.getBody();
            assertThat(order).isNotNull();
            long id = order.id();

            // server-owned pricing: 6.50 + 3.50 meal + 0.90 cheese + (2.25 + 1.80 Large) = 14.95
            assertThat(order.subtotal()).isEqualByComparingTo("14.95");
            assertThat(order.tax()).isEqualByComparingTo("1.27");
            assertThat(order.total()).isEqualByComparingTo("16.22");

            // drive the lifecycle
            postForId("/" + id + "/advance"); // NEW -> PREP
            postForId("/" + id + "/advance"); // PREP -> READY
            postForId("/" + id + "/collect"); // READY -> DONE

            // and observe the matching broadcasts in order
            assertThat(nextEventType()).isEqualTo("ORDER_CREATED");
            assertThat(nextEventType()).isEqualTo("ORDER_STARTED");
            assertThat(nextEventType()).isEqualTo("ORDER_READY");
            assertThat(nextEventType()).isEqualTo("ORDER_COLLECTED");

            // final REST state agrees
            ResponseEntity<OrderResponse> fetched =
                    rest.getForEntity("/api/orders/" + id, OrderResponse.class);
            assertThat(fetched.getBody().status().name()).isEqualTo("DONE");
        } finally {
            session.disconnect();
        }
    }
}
