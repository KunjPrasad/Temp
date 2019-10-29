package com.example.demo.spring.boot.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * This class modifies the response body, adding timing information, if so instructed by incoming request
 * 
 * @author KunjPrasad
 *
 */
@ControllerAdvice
@Order(value = Ordered.LOWEST_PRECEDENCE)
public class BasePerfTimeHandler implements ResponseBodyAdvice<BaseDTO> {

    // header queried from request to identify if timing information should be provided
    static final String SHOW_TIME_HEADER = "X-PERF-TIME";

    /**
     * This method checks if the response is derived from BaseDTO. If so, then timing values can be added
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return BaseDTO.class.isAssignableFrom(returnType.getMethod().getReturnType());
    }

    /**
     * This method writes different time in body before sending it out
     */
    @Override
    public BaseDTO beforeBodyWrite(BaseDTO body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        // return if body is null.. in case empty response is being returned
        if (body == null) {
            return body;
        }
        // get request object
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest req = sra.getRequest();
        // don't do any processing if correct header not given
        if (req.getHeader(SHOW_TIME_HEADER) == null) {
            return body;
        }
        // get values from attributes and make "BaseTime" object
        boolean found = false;
        BaseDTO.BaseTime baseTime = new BaseDTO.BaseTime();
        if (req.getAttribute(BaseDTO.BaseTime.START_TIME_ATTR_NM) != null) {
            baseTime.set_startTime((long) req.getAttribute(BaseDTO.BaseTime.START_TIME_ATTR_NM));
            found = true;
        }
        if (req.getAttribute(BaseDTO.BaseTime.SERVICE_START_TIME_ATTR_NM) != null) {
            baseTime.set_serviceStartTime((long) req.getAttribute(BaseDTO.BaseTime.SERVICE_START_TIME_ATTR_NM));
            found = true;
        }
        if (req.getAttribute(BaseDTO.BaseTime.SERVICE_END_TIME_ATTR_NM) != null) {
            baseTime.set_serviceEndTime((long) req.getAttribute(BaseDTO.BaseTime.SERVICE_END_TIME_ATTR_NM));
            found = true;
        }
        // finally add a time at end of base-time-handler
        baseTime.set_endTime(System.currentTimeMillis());
        // put "BaseTime" in response
        if (found) {
            body.set_time(baseTime);
        }
        // return response
        return body;
    }

}
