package com.ember.web;

import com.ember.service.TimeClockService;
import com.ember.web.dto.TimeEntryResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Time-clock for the signed-in staff member (any role). */
@RestController
@RequestMapping("/api/timeclock")
public class TimeClockController {

    private final TimeClockService clock;

    public TimeClockController(TimeClockService clock) {
        this.clock = clock;
    }

    @PostMapping("/in")
    public TimeEntryResponse clockIn(Authentication caller) {
        return clock.clockIn(caller.getName());
    }

    @PostMapping("/out")
    public TimeEntryResponse clockOut(Authentication caller) {
        return clock.clockOut(caller.getName());
    }

    /** The caller's open punch, or an empty body if not clocked in. */
    @GetMapping("/me")
    public TimeEntryResponse current(Authentication caller) {
        return clock.current(caller.getName());
    }
}
