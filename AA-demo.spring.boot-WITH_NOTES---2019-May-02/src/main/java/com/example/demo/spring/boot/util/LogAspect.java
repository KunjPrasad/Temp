package com.example.demo.spring.boot.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.SourceLocation;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * An aspect-component to log details
 * 
 * @author KunjPrasad
 *
 */
@Aspect
@Component
@Slf4j
public class LogAspect {

    /**
     * Aspect to log at start and end of method
     * 
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    // @Around("@annotation(Log)")
    @Around("execution(* getTestMessage(..))")
    // @Around("execution(* getTestMessage(..))")
    public Object logExecutionTime(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        boolean normalTermination = false;

        Signature sig = proceedingJoinPoint.getSignature();
        // for class names, see
        // https://stackoverflow.com/questions/15202997/what-is-the-difference-between-canonical-name-simple-name-and-class-name-in-jav
        String sigDeclTypeName = sig.getDeclaringTypeName();
        String sigName = sig.getName();
        String sigDeclType_CanNm = sig.getDeclaringType().getCanonicalName();

        SourceLocation sl = proceedingJoinPoint.getSourceLocation();

        long startTime = System.currentTimeMillis();

        try {
            log.info("START");
            log.info("==========");
            log.info("JoinPoint: class={}", proceedingJoinPoint.getClass());
            log.info("signature: sigDeclTypeName={}, sigName={}, sigDeclType_CanNm={}", sigDeclTypeName, sigName,
                    sigDeclType_CanNm);
            log.info("sourceLocation: class={}, withinType={}", sl.getClass(), sl.getWithinType());
            log.info("this: class={}, object={}", proceedingJoinPoint.getThis().getClass(),
                    proceedingJoinPoint.getThis());
            log.info("target: class={}, object={}", proceedingJoinPoint.getTarget().getClass(),
                    proceedingJoinPoint.getTarget());
            log.info("==========");
            Object result = proceedingJoinPoint.proceed();
            normalTermination = true;
            return result;
        } catch (Throwable throwable) {
            // don't log the error as it is done by handler
            throw throwable;
        } finally {
            // ..the job of aspect is only to log the start, time-elapsed, and signal end
            long duration = System.currentTimeMillis() - startTime;
            log.info("END sigDeclTypeName={}, sigName={}, normalTermination={}, time(ms)={}", sigDeclTypeName, sigName,
                    normalTermination, duration);
        }
    }
}
