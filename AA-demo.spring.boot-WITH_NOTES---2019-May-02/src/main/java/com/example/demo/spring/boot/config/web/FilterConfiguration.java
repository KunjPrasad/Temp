package com.example.demo.spring.boot.config.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.spring.boot.filter.PreFilterTimeRegisteringFilter;
import com.example.demo.spring.boot.filter.RateLimitingFilter;

/**
 * This class controls the configuration of various filters
 * 
 * @author KunjPrasad
 *
 */
@Configuration
public class FilterConfiguration {

    @Value("${filter.rateLimit.enable}")
    private boolean enableRateLimit;

    /**
     * Registering filter to log time in request before hitting filter
     * 
     * @return
     */
    @Bean
    public FilterRegistrationBean<PreFilterTimeRegisteringFilter> preFilterTimeRegisteringFilterRegistration() {
        FilterRegistrationBean<PreFilterTimeRegisteringFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new PreFilterTimeRegisteringFilter());
        registration.addUrlPatterns("/*");
        registration.setName("preFilterTimeRegisteringFilter");
        registration.setEnabled(true);
        // set lowest value so that this is first filter
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }

    /**
     * Registering filter for rate limiter
     * 
     * @return
     */
    // **VERY VERY IMPORTANT**: It is observed, in rateLimitingFilter's registration bean.. that simply setting
    // enabled(false) for test environment somehow still does not work!! So the other alternative is to simply map the
    // filter to a non-existing url!
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration() {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitingFilter());
        // disable rate limiting, for example in test profile, because many calls will be made one after other
        if (enableRateLimit) {
            registration.addUrlPatterns("/*");
        } else {
            registration.addUrlPatterns("/absent-url-so-the-filter-does-not-interfere");
        }
        registration.setName("rateLimitingFilter");
        registration.setOrder(2);
        registration.setEnabled(true);
        // This does not work: registration.setEnabled(enableRateLimit); -- ideally it should get disabled in test
        // environment, but it still shows up!!
        return registration;
    }
}
