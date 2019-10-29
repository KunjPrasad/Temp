package com.example.demo.spring.boot.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * This enum lays out various log-levels for an exception
 * 
 * @author KunjPrasad
 *
 */
@Getter
@Slf4j
public enum ExceptionLogLevel {

    INFO("Basic") {
        @Override
        public void log(String logStr, Throwable e) {
            log.info(logStr, e);
        }
    },
    WARN("Warning") {
        @Override
        public void log(String logStr, Throwable e) {
            log.warn(logStr, e);
        }
    },
    ERROR("Major") {
        @Override
        public void log(String logStr, Throwable e) {
            log.error(logStr, e);
        }
    },
    // "Fatal" only for codeIntegrationException and UnexpectedEception
    FATAL("Fatal") {
        @Override
        public void log(String logStr, Throwable e) {
            log.error(logStr, e);
        }
    };

    private String responseSeverity;

    ExceptionLogLevel(String responseSeverity) {
        this.responseSeverity = responseSeverity;
    }

    public abstract void log(String logStr, Throwable e);
}
