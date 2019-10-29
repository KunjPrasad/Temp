package com.example.demo.spring.boot.dto;

import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/**
 * This method takes 2 requestParam strings and join them into an object
 * 
 * @author KunjPrasad
 *
 */
@Component
public class DualMessageDTOMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Resource(name = "dualMessageDTOValidator")
    private Validator dualMessageDTOValidator;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(DualMessageDTO.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        // get path param binding from request
        HttpServletRequest httpServletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        Map<String, String> pathParamMap = (Map<String, String>) httpServletRequest
                .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        // make dto bject to be returned
        DualMessageDTO dto = new DualMessageDTO();
        dto.setMsg1(pathParamMap.get("msgx"));
        dto.setMsg2(pathParamMap.get("msgy"));
        /* If using in real code and need to use validation, add logic here..
        // invoke validation if configured
        if (parameter.hasParameterAnnotation(Valid.class)) {
            ...
        }
        */
        // return
        return dto;
    }

}
