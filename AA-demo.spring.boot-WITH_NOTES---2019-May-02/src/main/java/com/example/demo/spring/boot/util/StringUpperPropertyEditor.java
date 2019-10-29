package com.example.demo.spring.boot.util;

import java.beans.PropertyEditorSupport;

import org.springframework.stereotype.Component;

/**
 * Custom editor to change property of string inputs from web request
 * 
 * @author KunjPrasad
 *
 */
@Component
public class StringUpperPropertyEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
        this.setValue(text.toUpperCase());
    }

}
