package com.ember.web.dto;

/** Issued token plus who it belongs to and their role, for the client to store. */
public record LoginResponse(
        String token,
        String username,
        String role
) { }
