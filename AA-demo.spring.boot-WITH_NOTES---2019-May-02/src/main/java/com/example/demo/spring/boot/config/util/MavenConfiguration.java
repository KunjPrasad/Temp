package com.example.demo.spring.boot.config.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * This class holds maven properties made available to application
 * 
 * @author KunjPrasad
 *
 */
@Configuration
@PropertySource("classpath:maven.properties")
public class MavenConfiguration {

    @Autowired
    private Environment env;

    public String getMavenProperty(String propertyName) {
        return env.getProperty(propertyName);
    }
}
