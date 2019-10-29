package com.example.demo.spring.boot.exception;

import org.springframework.http.HttpStatus;

public class Response500Exception extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public Response500Exception(Exception causeException, String returnMessage, String detailCauseMessage,
            ExceptionLogLevel excpLogLevel) {
        super(causeException, HttpStatus.INTERNAL_SERVER_ERROR, returnMessage, detailCauseMessage, excpLogLevel);
    }
}
