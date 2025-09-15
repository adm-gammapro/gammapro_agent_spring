package com.gammapro.agent.error;

public record ErrorResponse(String error, String detail) {
    public static ErrorResponse of(String error) {
        return new ErrorResponse(error, null);
    }
    public static ErrorResponse of(String error, String detail) {
        return new ErrorResponse(error, detail);
    }
}
