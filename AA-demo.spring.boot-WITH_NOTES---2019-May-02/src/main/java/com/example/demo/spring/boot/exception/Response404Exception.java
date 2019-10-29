package com.example.demo.spring.boot.exception;

import org.springframework.http.HttpStatus;

public class Response404Exception extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public Response404Exception(Exception causeException, String returnMessage, String detailCauseMessage,
            ExceptionLogLevel excpLogLevel) {
        super(causeException, HttpStatus.NOT_FOUND, returnMessage, detailCauseMessage, excpLogLevel);
    }
}
