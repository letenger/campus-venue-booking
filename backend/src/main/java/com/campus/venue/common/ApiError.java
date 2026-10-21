package com.campus.venue.common;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error envelope: { success:false, error:{ code, message, details } }
 */
public class ApiError {
    private boolean success = false;
    private ErrorBody error;
    private Instant timestamp;

    public ApiError(String code, String message, List<String> details) {
        this.error = new ErrorBody(code, message, details);
        this.timestamp = Instant.now();
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    public static ApiError of(String code, String message, List<String> details) {
        return new ApiError(code, message, details);
    }

    public boolean isSuccess() { return success; }
    public ErrorBody getError() { return error; }
    public Instant getTimestamp() { return timestamp; }

    public static class ErrorBody {
        private String code;
        private String message;
        private List<String> details;

        public ErrorBody(String code, String message, List<String> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
        public List<String> getDetails() { return details; }
    }
}
