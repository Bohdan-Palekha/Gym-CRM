package com.gymcrm.workload.aspect;

import com.gymcrm.workload.repository.TrainerWorkloadRepository;
import com.gymcrm.workload.service.TrainerWorkloadServiceImpl;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingAspect — unit tests")
class LoggingAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature methodSignature;
    @Mock
    private TrainerWorkloadRepository repository;

    private final LoggingAspect aspect = new LoggingAspect();

    private TrainerWorkloadServiceImpl serviceTarget;

    static class RepositoryLikeTarget {
        public String find(String id) {
            return "found:" + id;
        }
    }

    private final RepositoryLikeTarget repositoryTarget = new RepositoryLikeTarget();

    @BeforeEach
    void setUp() {
        //serviceTarget stands in as "some object in the .service package"
        serviceTarget = new TrainerWorkloadServiceImpl(repository, 5);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("someMethod");
    }

    @Nested
    @DisplayName("normal execution")
    class NormalExecution {

        @Test
        @DisplayName("proceed() is called exactly once and its result is returned unchanged")
        void proceedsAndReturnsResult() throws Throwable {
            when(joinPoint.getTarget()).thenReturn(repositoryTarget);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"abc"});
            when(joinPoint.proceed()).thenReturn("the-result");

            Object result = aspect.logMethodExecution(joinPoint);

            assertEquals("the-result", result);
            verify(joinPoint, times(1)).proceed();
        }

        @Test
        @DisplayName("null args are formatted as 'none' without throwing")
        void nullArgs_doesNotThrow() throws Throwable {
            when(joinPoint.getTarget()).thenReturn(repositoryTarget);
            when(joinPoint.getArgs()).thenReturn(null);
            when(joinPoint.proceed()).thenReturn("ok");

            assertDoesNotThrow(() -> aspect.logMethodExecution(joinPoint));
        }

        @Test
        @DisplayName("a .service-package target (INFO branch) executes without error")
        void servicePackageTarget_doesNotThrow() throws Throwable {
            when(joinPoint.getTarget()).thenReturn(serviceTarget);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"bob.trainer"});
            when(joinPoint.proceed()).thenReturn("ok");

            assertDoesNotThrow(() -> aspect.logMethodExecution(joinPoint));
        }
    }

    @Nested
    @DisplayName("special result formatting")
    class ResultFormatting {

        @Test
        @DisplayName("an empty Optional result is still returned unchanged (WARN path)")
        void emptyOptional_returnedUnchanged() throws Throwable {
            when(joinPoint.getTarget()).thenReturn(repositoryTarget);
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.proceed()).thenReturn(Optional.empty());

            Object result = aspect.logMethodExecution(joinPoint);

            assertInstanceOf(Optional.class, result);
            assertTrue(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("a present Optional result is returned unchanged")
        void presentOptional_returnedUnchanged() throws Throwable {
            when(joinPoint.getTarget()).thenReturn(repositoryTarget);
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            Optional<String> present = Optional.of("value");
            when(joinPoint.proceed()).thenReturn(present);

            assertSame(present, aspect.logMethodExecution(joinPoint));
        }

        @Test
        @DisplayName("a ResponseEntity result does not throw while being formatted for the log line")
        void responseEntityResult_doesNotThrow() throws Throwable {
            when(joinPoint.getTarget()).thenReturn(repositoryTarget);
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.proceed()).thenReturn(ResponseEntity.ok("body"));

            assertDoesNotThrow(() -> aspect.logMethodExecution(joinPoint));
        }

        @Test
        @DisplayName("a Collection result does not throw while being formatted for the log line")
        void collectionResult_doesNotThrow() throws Throwable {
            when(joinPoint.getTarget()).thenReturn(repositoryTarget);
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.proceed()).thenReturn(List.of("a", "b", "c"));

            assertDoesNotThrow(() -> aspect.logMethodExecution(joinPoint));
        }

        @Test
        @DisplayName("a null result is formatted as 'null' without throwing")
        void nullResult_doesNotThrow() throws Throwable {
            when(joinPoint.getTarget()).thenReturn(repositoryTarget);
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.proceed()).thenReturn(null);

            assertNull(aspect.logMethodExecution(joinPoint));
        }
    }

    @Nested
    @DisplayName("exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("an exception from proceed() is always re-thrown, unchanged")
        void rethrowsException() throws Throwable {
            when(joinPoint.getTarget()).thenReturn(repositoryTarget);
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            RuntimeException boom = new RuntimeException("downstream failure");
            when(joinPoint.proceed()).thenThrow(boom);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> aspect.logMethodExecution(joinPoint));
            assertSame(boom, thrown);
        }

        @Test
        @DisplayName("proceed() was called before the exception propagated")
        void proceedCalledBeforeRethrow() throws Throwable {
            when(joinPoint.getTarget()).thenReturn(repositoryTarget);
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.proceed()).thenThrow(new IllegalStateException("oops"));

            assertThrows(IllegalStateException.class, () -> aspect.logMethodExecution(joinPoint));
            verify(joinPoint).proceed();
        }
    }
}
