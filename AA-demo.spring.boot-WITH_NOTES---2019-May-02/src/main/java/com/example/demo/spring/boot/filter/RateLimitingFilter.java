package com.example.demo.spring.boot.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;

import com.google.common.util.concurrent.RateLimiter;

import lombok.extern.slf4j.Slf4j;

/**
 * This filter class is used to rate limit incoming requests
 * 
 * @author KunjPrasad
 *
 */
@Slf4j
public class RateLimitingFilter implements Filter {

    static final RateLimiter absentUrlRegexLimiter = RateLimiter.create(0.5); // 1 per 2 sec
    static final long limitAcquireTimeoutInMs = 100;

    static final Map<String, RateLimiter> urlRegexLimiterMap;

    static {
        urlRegexLimiterMap = new HashMap<>();
        urlRegexLimiterMap.put("/test/get1/.*", RateLimiter.create(0.2)); // 1 per 5 sec
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        System.out.println("~~~calling ratelimit" + path);
        boolean foundMatch = false;
        // do filter logic
        for (Entry<String, RateLimiter> entry : urlRegexLimiterMap.entrySet()) {
            if (path.matches(entry.getKey())) {
                foundMatch = true;
                limitUsingLimiter(entry.getValue(), httpRequest, httpResponse);
                break;
            }
        }
        // if no match found, then implement default logic
        if (!foundMatch) {
            limitUsingLimiter(absentUrlRegexLimiter, httpRequest, httpResponse);
        }
        // pass along in chain
        chain.doFilter(request, response);
    }

    // Utility method to limit calls using corresponding limiter
    void limitUsingLimiter(RateLimiter rateLimiter, HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws IOException {
        if (!rateLimiter.tryAcquire(1, limitAcquireTimeoutInMs, TimeUnit.MILLISECONDS)) {
            log.info("rateLimiter acquire failure, url={}, allowedRate={}", httpRequest.getRequestURI(),
                    rateLimiter.getRate());
            httpResponse.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.flushBuffer();
        }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }
}
