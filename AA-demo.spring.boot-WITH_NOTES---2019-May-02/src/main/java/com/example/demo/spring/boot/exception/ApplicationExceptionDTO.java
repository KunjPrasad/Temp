package com.example.demo.spring.boot.exception;

import com.example.demo.spring.boot.util.BaseDTO;

import lombok.Getter;
import lombok.Setter;

/**
 * This class represents the DTO class used to convey exception information
 * 
 * @author KunjPrasad
 *
 */
@Getter
@Setter
public class ApplicationExceptionDTO extends BaseDTO {
    private int responseStatus;
    private String returnMessage;
    private String errorSeverity;
}
