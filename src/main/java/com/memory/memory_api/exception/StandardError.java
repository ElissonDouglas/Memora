package com.memory.memory_api.exception;

import java.time.Instant;
import java.util.List;

public record StandardError(Instant timestamp,
                            Integer status,
                            String error,
                            String message,
                            String path,
                            List<String> details) {
    public StandardError(Instant timestamp, Integer status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }
}
