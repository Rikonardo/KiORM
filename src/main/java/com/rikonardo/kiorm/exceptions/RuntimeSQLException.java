package com.rikonardo.kiorm.exceptions;

public class RuntimeSQLException extends RuntimeException {
    public RuntimeSQLException(Exception e) {
        super(e);
    }
}
