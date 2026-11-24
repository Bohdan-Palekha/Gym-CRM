package com.gymcrm.aspect;

import com.gymcrm.annotation.Sensitive;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

//info — service/facade layer entry + exit (business operations)
//debug — dao/util layer entry + exit (infrastructure)
//warn  — any method returns empty optional (unexpected absence)
//error — any exception thrown (with full stack trace)
@Aspect
@Component
public class LoggingAspect {

    private static final String SENSITIVE_MASK = "***SENSITIVE***";
    private static final String EMPTY_OPTIONAL = "Optional.empty";


    @Pointcut(Pointcuts.SERVICE)
    private void serviceLayer() {}

    @Pointcut(Pointcuts.DAO)
    private void daoLayer() {}

    @Pointcut(Pointcuts.FACADE)
    private void facadeLayer() {}

    @Pointcut(Pointcuts.UTIL)
    private void utilLayer() {}

    @Pointcut(Pointcuts.CONTROLLER)
    private void controllerLayer() {}

    @Pointcut("serviceLayer() || daoLayer() || facadeLayer() || utilLayer() || controllerLayer()")
    private void applicationLayer() {}

    // around
    @Around("applicationLayer()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {

        // dynamic logger, shows class name
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        Object[] args = joinPoint.getArgs();
        boolean isInfo = isInfoLevel(joinPoint);
        boolean isSensitive = isSensitiveMethod(joinPoint, signature);

        long start = System.currentTimeMillis();

        // entry log — mask both args and result for @Sensitive methods (e.g. password params)
        String argsStr = isSensitive ? SENSITIVE_MASK : formatArgs(args);
        if (isInfo) {
            log.info("→ {}() | args: [{}]", methodName, argsStr);
        } else {
            log.debug("→ {}() | args: [{}]", methodName, argsStr);
        }

        try {
            Object result   = joinPoint.proceed();
            long   duration = System.currentTimeMillis() - start;
            if (isEmptyOptional(result)) {
                log.warn("← {}() | {}ms | returned empty Optional (entity not found)",
                        methodName, duration);
                return result;
            }

            // log for normal exit
            String resultStr = isSensitive ? SENSITIVE_MASK : formatResult(result);
            if (isInfo) {
                log.info("← {}() | {}ms | result: [{}]", methodName, duration, resultStr);
            } else {
                log.debug("← {}() | {}ms | result: [{}]", methodName, duration, resultStr);
            }

            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            // error with throwable as last arg → SLF4J adds full stack trace
            log.error("✗ {}() | {}ms | threw {} — {}",
                    methodName, duration,
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw ex; //rethrowing
        }
    }

    //service and controller log; at debug shows dao, facade, util logs
    private boolean isInfoLevel(ProceedingJoinPoint joinPoint) {
        String packageName = joinPoint.getTarget().getClass().getPackageName();
        return packageName.contains(".service") || packageName.contains(".controller");
    }


    private boolean isSensitiveMethod(ProceedingJoinPoint joinPoint,
                                      MethodSignature signature) {
        Method concrete = AopUtils.getMostSpecificMethod(
                signature.getMethod(),
                joinPoint.getTarget().getClass());
        return concrete.isAnnotationPresent(Sensitive.class);
    }

    private boolean isEmptyOptional(Object result) {
        return result instanceof Optional<?> opt && opt.isEmpty();
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) return "none";
        return Arrays.stream(args)
                .map(this::formatValue)
                .collect(Collectors.joining(", "));
    }

    private String formatResult(Object result) {
        if (result == null) return "null";
        return formatValue(result);
    }

    //converting values to strings & truncating collections
    //users have password free toString
    private String formatValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Optional<?> opt) {
            return opt.map(v -> "Optional[" + v + "]").orElse(EMPTY_OPTIONAL);
        }
        if (value instanceof org.springframework.http.ResponseEntity<?> response) {
            return "ResponseEntity(status=" + response.getStatusCode().value()
                    + ", body=" + formatValue(response.getBody()) + ")";
        }
        if (value instanceof Collection<?> col) {
            return col.getClass().getSimpleName() + "(size=" + col.size() + ")";
        }
        return value.toString();
    }
}
