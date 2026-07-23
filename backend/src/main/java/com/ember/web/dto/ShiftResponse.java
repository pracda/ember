package com.ember.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** A scheduled shift with the staff member's name resolved for display. */
public record ShiftResponse(
        Long id,
        Long staffId,
        String staffName,
        LocalDate workDate,
        LocalTime startTime,
        LocalTime endTime,
        String note
) { }
