package com.example.demo.spring.boot.exception;

public class CodeIntegrationException extends Response500Exception {

    private static final long serialVersionUID = 1L;

    public CodeIntegrationException(Exception causeException, String returnMessage, String detailCauseMessage) {
        super(causeException, returnMessage, detailCauseMessage, ExceptionLogLevel.FATAL);
    }
}
