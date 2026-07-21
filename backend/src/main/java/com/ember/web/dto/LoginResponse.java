package com.ember.web.dto;

/** Issued token plus who it belongs to, for the client to store. */
public record LoginResponse(
        String token,
        String username,
        String role,
        String displayName
) { }
