package com.example.demo.spring.boot.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.example.demo.spring.boot.util.BaseDTO;

/**
 * This filter adds the a "time" to incoming request This is identified as the first time that can be recorded
 * 
 * @author KunjPrasad
 *
 */
public class PreFilterTimeRegisteringFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        // add time in local request object
        req.setAttribute(BaseDTO.BaseTime.START_TIME_ATTR_NM, System.currentTimeMillis());
        // pass the request in chain
        chain.doFilter(req, resp);
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {}

    @Override
    public void destroy() {}
}
