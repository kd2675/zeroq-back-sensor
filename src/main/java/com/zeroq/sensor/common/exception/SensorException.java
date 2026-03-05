package com.zeroq.sensor.common.exception;

import lombok.Getter;

@Getter
public class SensorException extends RuntimeException {
    private final String code;
    private final int status;

    public SensorException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static class ResourceNotFoundException extends SensorException {
        public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
            super(
                    "RESOURCE_NOT_FOUND",
                    String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue),
                    404
            );
        }
    }

    public static class ValidationException extends SensorException {
        public ValidationException(String message) {
            super("VALIDATION_ERROR", message, 400);
        }
    }

    public static class ConflictException extends SensorException {
        public ConflictException(String message) {
            super("CONFLICT", message, 409);
        }
    }

    public static class ForbiddenException extends SensorException {
        public ForbiddenException(String message) {
            super("FORBIDDEN", message, 403);
        }
    }
}
