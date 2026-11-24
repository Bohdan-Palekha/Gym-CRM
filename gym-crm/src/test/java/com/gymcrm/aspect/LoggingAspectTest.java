package com.gymcrm.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingAspect — unit tests")
class LoggingAspectTest {

    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private MethodSignature     methodSignature;

    private LoggingAspect aspect;

    static class SampleService {
        public String doWork(String input) { return "result:" + input; }
        public void doVoid() {}
        public Optional<String> findById(Long id) { return Optional.empty(); }
    }

    @BeforeEach
    void setUp() throws Exception {
        aspect = new LoggingAspect();
        SampleService target = new SampleService();
        Method        method = SampleService.class.getMethod("doWork", String.class);

        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("doWork");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"hello"});
    }

    @Nested
    @DisplayName("Normal execution")
    class NormalExecution {

        @Test
        @DisplayName("proceed() is called exactly once")
        void callsProceedExactlyOnce() throws Throwable {
            when(joinPoint.proceed()).thenReturn("result:hello");
            aspect.logMethodExecution(joinPoint);
            verify(joinPoint, times(1)).proceed();
        }

        @Test
        @DisplayName("returns the real method result unchanged")
        void returnsRealResult() throws Throwable {
            when(joinPoint.proceed()).thenReturn("result:hello");
            Object returned = aspect.logMethodExecution(joinPoint);
            assertEquals("result:hello", returned);
        }

        @Test
        @DisplayName("null return value (void method) is returned as null")
        void voidMethodReturnsNull() throws Throwable {
            when(joinPoint.proceed()).thenReturn(null);
            assertNull(aspect.logMethodExecution(joinPoint));
        }

        @Test
        @DisplayName("non-empty Optional result is returned unchanged")
        void nonEmptyOptionalReturnedUnchanged() throws Throwable {
            Optional<String> present = Optional.of("found");
            when(joinPoint.proceed()).thenReturn(present);
            Object result = aspect.logMethodExecution(joinPoint);
            assertSame(present, result);
        }
    }

    @Nested
    @DisplayName("Empty Optional result — WARN path")
    class EmptyOptionalResult {

        @BeforeEach
        void setUpFindByIdMethod() throws Exception {
            Method findByIdMethod = SampleService.class
                    .getMethod("findById", Long.class);
            when(methodSignature.getName()).thenReturn("findById");
            when(methodSignature.getMethod()).thenReturn(findByIdMethod);
            when(joinPoint.getArgs()).thenReturn(new Object[]{999L});
        }

        @Test
        @DisplayName("empty Optional result is returned unchanged by the aspect")
        void emptyOptional_returnedUnchanged() throws Throwable {
            when(joinPoint.proceed()).thenReturn(Optional.empty());
            Object result = aspect.logMethodExecution(joinPoint);
            assertInstanceOf(Optional.class, result);
            assertTrue(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("proceed() is still called for empty Optional result")
        void emptyOptional_proceedCalled() throws Throwable {
            when(joinPoint.proceed()).thenReturn(Optional.empty());
            aspect.logMethodExecution(joinPoint);
            verify(joinPoint, times(1)).proceed();
        }
    }

    @Nested
    @DisplayName("Exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("RuntimeException from proceed() is always re-thrown")
        void rethrowsRuntimeException() throws Throwable {
            RuntimeException boom = new RuntimeException("downstream failure");
            when(joinPoint.proceed()).thenThrow(boom);
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> aspect.logMethodExecution(joinPoint));
            assertSame(boom, thrown, "Must re-throw the exact same exception instance");
        }

        @Test
        @DisplayName("checked Exception from proceed() is always re-thrown")
        void rethrowsCheckedException() throws Throwable {
            Exception checked = new Exception("checked failure");
            when(joinPoint.proceed()).thenThrow(checked);
            Throwable thrown = assertThrows(Exception.class,
                    () -> aspect.logMethodExecution(joinPoint));
            assertSame(checked, thrown);
        }

        @Test
        @DisplayName("proceed() was called before the exception was re-thrown")
        void proceedCalledBeforeRethrow() throws Throwable {
            when(joinPoint.proceed()).thenThrow(new IllegalStateException("oops"));
            assertThrows(IllegalStateException.class,
                    () -> aspect.logMethodExecution(joinPoint));
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("aspect never swallows exceptions — caller always receives them")
        void neverSwallowsException() throws Throwable {
            Error error = new OutOfMemoryError("oom");
            when(joinPoint.proceed()).thenThrow(error);
            assertThrows(OutOfMemoryError.class,
                    () -> aspect.logMethodExecution(joinPoint));
        }
    }
}
