package com.example.demo.spring.boot.util;

import java.util.ArrayList;

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
 * This class modifies the response body, adding user-story information, if so instructed by incoming request
 * 
 * @author KunjPrasad
 *
 */

@ControllerAdvice
@Order(value = Ordered.LOWEST_PRECEDENCE - 1)
public class BaseUserStoryHandler implements ResponseBodyAdvice<BaseDTO> {

    // header queried from request to identify if timing information should be provided
    static final String SHOW_USER_STORY_HEADER = "X-USER-STORY";

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
        if (req.getHeader(SHOW_USER_STORY_HEADER) == null) {
            return body;
        }
        // get values from attributes and make "BaseUserStory" object
        if (req.getAttribute(BaseDTO.BaseUserStory.USER_STORY_ATTR_NM) != null) {
            BaseDTO.BaseUserStory baseUserStory = new BaseDTO.BaseUserStory();
            baseUserStory.set_userStoryBag(new ArrayList<String>());
            for (String str : (String[]) req.getAttribute(BaseDTO.BaseUserStory.USER_STORY_ATTR_NM)) {
                baseUserStory.get_userStoryBag().add(str);
            }
            body.set_userStory(baseUserStory);
        }
        // return response
        return body;
    }

}
