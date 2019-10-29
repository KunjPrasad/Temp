package com.example.demo.spring.boot.exception;

import org.springframework.http.HttpStatus;

public class Response400Exception extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public Response400Exception(Exception causeException, String returnMessage, String detailCauseMessage,
            ExceptionLogLevel excpLogLevel) {
        super(causeException, HttpStatus.BAD_REQUEST, returnMessage, detailCauseMessage, excpLogLevel);
    }
}
