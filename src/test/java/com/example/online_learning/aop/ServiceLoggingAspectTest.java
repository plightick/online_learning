package com.example.online_learning.aop;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StopWatch;

class ServiceLoggingAspectTest {

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(ServiceLoggingAspect.class);

    private final ServiceLoggingAspect aspect = new ServiceLoggingAspect();

    private Level originalLevel;

    @BeforeEach
    void rememberLoggerLevel() {
        originalLevel = LOGGER.getLevel();
    }

    @AfterEach
    void restoreLoggerLevel() {
        LOGGER.setLevel(originalLevel);
    }

    @Test
    void serviceMethodsPointcutShouldBeCallable() {
        assertDoesNotThrow(aspect::serviceMethods);
    }

    @Test
    void logExecutionTimeShouldReturnJoinPointResultWhenDebugIsDisabled() throws Throwable {
        LOGGER.setLevel(Level.INFO);
        ProceedingJoinPoint joinPoint = joinPoint("runFast", "done");

        Object result = aspect.logExecutionTime(joinPoint);

        assertSame("done", result);
        verify(joinPoint).proceed();
    }

    @Test
    void logExecutionTimeShouldPropagateFailureWhenDebugIsEnabled() throws Throwable {
        LOGGER.setLevel(Level.DEBUG);
        ProceedingJoinPoint joinPoint = joinPointThrowing("runFail", new IllegalStateException("boom"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> aspect.logExecutionTime(joinPoint));

        assertEquals("boom", exception.getMessage());
        verify(joinPoint).proceed();
    }

    @Test
    void stopAndGetExecutionTimeShouldReturnZeroWhenStopWatchIsNotRunning() {
        Long executionTime = ReflectionTestUtils.invokeMethod(
                aspect,
                "stopAndGetExecutionTime",
                new StopWatch("idle"));

        assertEquals(0L, executionTime);
    }

    @Test
    void logExecutionResultShouldSupportAllSuccessThresholds() {
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(aspect, "logExecutionResult", "Demo.fast", 100L, true);
            ReflectionTestUtils.invokeMethod(aspect, "logExecutionResult", "Demo.slow", 700L, true);
            ReflectionTestUtils.invokeMethod(aspect, "logExecutionResult", "Demo.verySlow", 1500L, true);
        });
    }

    private static ProceedingJoinPoint joinPoint(String methodName, Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = baseJoinPoint(methodName);
        when(joinPoint.proceed()).thenReturn(result);
        return joinPoint;
    }

    private static ProceedingJoinPoint joinPointThrowing(
            String methodName,
            RuntimeException exception) throws Throwable {
        ProceedingJoinPoint joinPoint = baseJoinPoint(methodName);
        when(joinPoint.proceed()).thenThrow(exception);
        return joinPoint;
    }

    private static ProceedingJoinPoint baseJoinPoint(String methodName) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"demo"});
        when(signature.getName()).thenReturn(methodName);
        return joinPoint;
    }

    private static final class TestService {
    }
}
