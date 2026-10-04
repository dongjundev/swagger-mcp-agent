package com.example.swagger_center.store;

public class SpecFetchException extends RuntimeException {

    public SpecFetchException(String message) {
        super(message);
    }

    public SpecFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
