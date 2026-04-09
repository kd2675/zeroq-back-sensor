package com.zeroq.sensor.common.exception;

import web.common.core.response.base.exception.GeneralException;
import web.common.core.response.base.vo.Code;

public class SensorException extends GeneralException {

    public SensorException(Code errorCode, String message) {
        super(errorCode, message);
    }

    public Integer getCode() {
        return getErrorCode().getCode();
    }

    public int getStatus() {
        return getErrorCode().getHttpStatus().value();
    }

    public static class ResourceNotFoundException extends SensorException {
        public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
            super(
                    Code.NOT_FOUND,
                    String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue)
            );
        }
    }

    public static class ValidationException extends SensorException {
        public ValidationException(String message) {
            super(Code.VALIDATION_ERROR, message);
        }
    }

    public static class ConflictException extends SensorException {
        public ConflictException(String message) {
            super(Code.CONFLICT, message);
        }
    }

    public static class ForbiddenException extends SensorException {
        public ForbiddenException(String message) {
            super(Code.FORBIDDEN, message);
        }
    }
}
