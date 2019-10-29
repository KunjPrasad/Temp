package com.example.demo.spring.boot.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to include user stories and defects in request
 * 
 * @author KunjPrasad
 *
 */
@Target(value = { ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface UserStory {

    // list of stories associated with the method
    String[] stories();
}
