package com.example.demo.spring.boot.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * This is the base class of all exception types thrown in the application
 * 
 * @author KunjPrasad
 *
 */
@Getter
public class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // the http response status corresponding to exception
    private HttpStatus responseStatus;

    // the message returned to user
    private String returnMessage;

    // the detailed-cause that can be returned if configured; But mainly used for logging
    private String detailCauseMessage;

    // the log level
    private ExceptionLogLevel excpLogLevel;

    // not providing a no-arg constructor to force any exception-instantiation to provide compulsory values
    public ApplicationException(Exception causeException, HttpStatus responseStatus, String returnMessage,
            String detailCauseMessage, ExceptionLogLevel excpLogLevel) {
        super(causeException);
        this.responseStatus = responseStatus;
        this.returnMessage = returnMessage;
        this.detailCauseMessage = detailCauseMessage;
        this.excpLogLevel = excpLogLevel;
    }
}
