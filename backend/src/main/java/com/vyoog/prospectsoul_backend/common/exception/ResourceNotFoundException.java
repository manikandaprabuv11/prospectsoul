package com.vyoog.prospectsoul_backend.common.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String entityType, UUID id) {
        super(entityType + " not found: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
