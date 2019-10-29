package com.example.demo.spring.boot.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * An aspect-component to add user story in the request attribute
 * 
 * @author KunjPrasad
 *
 */
@Aspect
@Component
public class UserStoryAdderAspect {

    /**
     * Aspect to add user story in the request
     * 
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(UserStory)")
    public Object addUserStory(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // NOTE: There is an assumption used here that UserStory is always used on a method
        UserStory userStory = ((MethodSignature) proceedingJoinPoint.getSignature())
                .getMethod().getAnnotation(UserStory.class);
        // adding the start-time
        RequestContextHolder.getRequestAttributes().setAttribute(BaseDTO.BaseUserStory.USER_STORY_ATTR_NM,
                userStory.stories(), RequestAttributes.SCOPE_REQUEST);
        // proceeding with logic
        return proceedingJoinPoint.proceed();
    }
}
