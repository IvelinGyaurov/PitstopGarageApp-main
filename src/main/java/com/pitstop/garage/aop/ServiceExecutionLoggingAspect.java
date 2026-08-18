package com.pitstop.garage.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ServiceExecutionLoggingAspect {

    @Around("execution(* com.pitstop.garage..service..*(..)) || "
            + "execution(* com.pitstop.garage.parts.PartsAdminService.*(..))")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("{} completed in {} ms",
                    joinPoint.getSignature().toShortString(), System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.warn("{} failed after {} ms: {}",
                    joinPoint.getSignature().toShortString(), System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }
}
