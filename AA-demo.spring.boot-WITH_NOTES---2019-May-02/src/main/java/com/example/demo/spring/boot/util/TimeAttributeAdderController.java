package com.example.demo.spring.boot.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specialization of @TimeAttributeAdder with start and end name to use for controller
 * 
 * @author KunjPrasad
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@TimeAttributeAdder(startName = BaseDTO.BaseTime.SERVICE_START_TIME_ATTR_NM,
        endName = BaseDTO.BaseTime.SERVICE_END_TIME_ATTR_NM)
public @interface TimeAttributeAdderController {}
