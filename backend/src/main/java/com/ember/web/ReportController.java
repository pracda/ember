package com.ember.web;

import com.ember.config.EmberProperties;
import com.ember.service.ReportService;
import com.ember.web.dto.DaySummaryResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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
}
