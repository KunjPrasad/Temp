package com.example.demo.spring.boot.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * An aspect-component to add start and end time in the request
 * 
 * @author KunjPrasad
 *
 */
@Aspect
@Component
public class TimeAttributeAdderAspect {

    /**
     * Aspect to add start and end time in the request
     * 
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(TimeAttributeAdder) "
            + "|| @annotation(TimeAttributeAdderController) ")
    public Object addStartAndEndTime(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // NOTE: There is an assumption used here that TimeAttributeAdder is always used on a method
        // NOTE: Using AnnotationUtils to be able to find annotation on annotations!!
        TimeAttributeAdder timeAttributeAdder = AnnotationUtils.findAnnotation(
                ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod(), TimeAttributeAdder.class);
        /*
         * JUST FYI.. Another way to get request object is..
         * ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
         * HttpServletRequest req = sra.getRequest();
         */
        try {
            // adding the start-time
            RequestContextHolder.getRequestAttributes().setAttribute(timeAttributeAdder.startName(),
                    System.currentTimeMillis(), RequestAttributes.SCOPE_REQUEST);
            // proceeding with logic
            return proceedingJoinPoint.proceed();
        } finally {
            // adding the end-time
            RequestContextHolder.getRequestAttributes().setAttribute(timeAttributeAdder.endName(),
                    System.currentTimeMillis(), RequestAttributes.SCOPE_REQUEST);
        }
    }
}
