package com.example.demo.spring.boot.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to include time attribute property in request
 * 
 * @author KunjPrasad
 *
 */
@Target(value = { ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TimeAttributeAdder {

    // the name used as attribute key before method is started
    String startName();

    // the name used as attribute key after method ends
    String endName();
}
