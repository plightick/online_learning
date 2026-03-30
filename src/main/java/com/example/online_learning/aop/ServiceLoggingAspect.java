package com.example.online_learning.aop;

import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class ServiceLoggingAspect {

    private static final int SLOW_THRESHOLD_MS = 500;
    private static final int VERY_SLOW_THRESHOLD_MS = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {
    }

    @Around("serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;

        StopWatch stopWatch = new StopWatch(fullMethodName);
        boolean completedSuccessfully = false;

        stopWatch.start(fullMethodName);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "Executing method: {} with arguments: {}",
                    fullMethodName,
                    Arrays.toString(joinPoint.getArgs()));
        }

        try {
            Object result = joinPoint.proceed();
            completedSuccessfully = true;
            return result;
        } finally {
            long executionTime = stopAndGetExecutionTime(stopWatch);
            logExecutionResult(fullMethodName, executionTime, completedSuccessfully);
        }
    }

    private long stopAndGetExecutionTime(StopWatch stopWatch) {
        if (stopWatch.isRunning()) {
            stopWatch.stop();
        }
        return stopWatch.getTotalTimeMillis();
    }

    private void logExecutionResult(String fullMethodName, long executionTime, boolean completedSuccessfully) {
        if (!completedSuccessfully) {
            LOGGER.error("Method {} failed after {} ms", fullMethodName, executionTime);
            return;
        }

        if (executionTime > VERY_SLOW_THRESHOLD_MS) {
            LOGGER.warn(
                    "Method {} finished in {} ms (exceeds {} ms threshold)",
                    fullMethodName,
                    executionTime,
                    VERY_SLOW_THRESHOLD_MS);
        } else if (executionTime > SLOW_THRESHOLD_MS) {
            LOGGER.info("Method {} finished in {} ms", fullMethodName, executionTime);
        } else {
            LOGGER.debug("Method {} finished in {} ms", fullMethodName, executionTime);
        }
    }
}
