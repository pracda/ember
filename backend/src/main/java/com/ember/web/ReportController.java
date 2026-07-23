package com.ember.web;

import com.ember.config.EmberProperties;
import com.ember.service.ReportService;
import com.ember.web.dto.AnalyticsResponse;
import com.ember.web.dto.DaySummaryResponse;
import com.ember.web.dto.LaborRow;
import com.ember.web.dto.MenuItemResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Manager reporting. Protected as MANAGER-only by {@code SecurityConfig}. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reports;
    private final EmberProperties props;

    public ReportController(ReportService reports, EmberProperties props) {
        this.reports = reports;
        this.props = props;
    }

    /** {@code GET /api/reports/day-summary?date=YYYY-MM-DD} — defaults to today (outlet timezone). */
    @GetMapping("/day-summary")
    public DaySummaryResponse daySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reports.daySummary(date != null ? date : LocalDate.now(props.getTimezone()));
    }

    /**
     * {@code GET /api/reports/analytics?from=YYYY-MM-DD&to=YYYY-MM-DD} — the manager
     * dashboard. Both dates default to today (outlet timezone).
     */
    @GetMapping("/analytics")
    public AnalyticsResponse analytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate today = LocalDate.now(props.getTimezone());
        LocalDate start = from != null ? from : today;
        LocalDate end = to != null ? to : today;
        return reports.analytics(start, end);
    }

    /** {@code GET /api/reports/low-stock} — tracked items running low or out. */
    @GetMapping("/low-stock")
    public List<MenuItemResponse> lowStock() {
        return reports.lowStock();
    }

    /** {@code GET /api/reports/labor?from=&to=} — hours worked + sales per staff. */
    @GetMapping("/labor")
    public List<LaborRow> labor(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate today = LocalDate.now(props.getTimezone());
        return reports.labor(from != null ? from : today, to != null ? to : today);
    }
}
