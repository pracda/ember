package com.ember.web.dto;

import jakarta.validation.constraints.Size;

/** Optional reason attached to a void or refund (for the audit trail). */
public record ReasonRequest(
        @Size(max = 255) String reason
) { }
