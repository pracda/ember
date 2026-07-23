package com.ember.web.dto;

import java.time.Instant;

/** A time-clock punch. {@code clockOut} is null while the entry is open. */
public record TimeEntryResponse(
        Long id,
        Long staffId,
        Instant clockIn,
        Instant clockOut
) { }
