package com.ember.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** Manager payload to schedule or update a shift. */
public record ShiftRequest(
        @NotNull Long staffId,
        @NotNull LocalDate workDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Size(max = 255) String note
) { }
