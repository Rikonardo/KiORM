package com.rikonardo.kiorm.exceptions;

public class InvalidDocumentClassException extends RuntimeException {
    public InvalidDocumentClassException(String message) {
        super(message);
    }

    public InvalidDocumentClassException(String message, Exception e) {
        super(message, e);
    }
}
