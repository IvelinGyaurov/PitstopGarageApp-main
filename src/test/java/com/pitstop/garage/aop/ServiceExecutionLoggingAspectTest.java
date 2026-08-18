package com.pitstop.garage.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceExecutionLoggingAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @InjectMocks
    private ServiceExecutionLoggingAspect aspect;

    @Test
    void logExecution_returnsResultWhenProceedSucceeds() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("CarService.addCar(..)");
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.logExecution(joinPoint);

        assertEquals("ok", result);
        verify(joinPoint).proceed();
    }

    @Test
    void logExecution_rethrowsWhenProceedFails() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("CarService.addCar(..)");
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> aspect.logExecution(joinPoint));
    }
}
