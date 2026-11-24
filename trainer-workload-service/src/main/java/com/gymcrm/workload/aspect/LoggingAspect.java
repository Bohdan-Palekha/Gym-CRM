package com.gymcrm.workload.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

//mirrors gym-crm's aspect.LoggingAspect
//log every service/controller call and outcome, one level down from TransactionLoggingFilter's per-request summary
//no @Sensitive since there no cred in payload
//info  = service/controller layer entry
//debug = repository layer entry + exit
//error = any exception thrown
@Aspect
@Component
public class LoggingAspect {

    private static final String EMPTY_OPTIONAL = "Optional.empty";

    @Pointcut(Pointcuts.SERVICE)
    private void serviceLayer() {
    }

    @Pointcut(Pointcuts.REPOSITORY)
    private void repositoryLayer() {
    }

    @Pointcut(Pointcuts.CONTROLLER)
    private void controllerLayer() {
    }

    @Pointcut("serviceLayer() || repositoryLayer() || controllerLayer()")
    private void applicationLayer() {
    }

    @Around("applicationLayer()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        Object[] args = joinPoint.getArgs();
        boolean isInfo = isInfoLevel(joinPoint);

        long start = System.currentTimeMillis();

        String argsStr = formatArgs(args);
        if (isInfo) {
            log.info("→ {}() | args: [{}]", methodName, argsStr);
        } else {
            log.debug("→ {}() | args: [{}]", methodName, argsStr);
        }

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            if (isEmptyOptional(result)) {
                log.warn("← {}() | {}ms | returned empty Optional (entity not found)", methodName, duration);
                return result;
            }

            String resultStr = formatResult(result);
            if (isInfo) {
                log.info("← {}() | {}ms | result: [{}]", methodName, duration, resultStr);
            } else {
                log.debug("← {}() | {}ms | result: [{}]", methodName, duration, resultStr);
            }
            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("✗ {}() | {}ms | threw {} — {}",
                    methodName, duration, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    private boolean isInfoLevel(ProceedingJoinPoint joinPoint) {
        String packageName = joinPoint.getTarget().getClass().getPackageName();
        return packageName.contains(".service") || packageName.contains(".controller");
    }

    private boolean isEmptyOptional(Object result) {
        return result instanceof Optional<?> opt && opt.isEmpty();
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "none";
        }
        return Arrays.stream(args).map(this::formatValue).collect(Collectors.joining(", "));
    }

    private String formatResult(Object result) {
        return result == null ? "null" : formatValue(result);
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
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
