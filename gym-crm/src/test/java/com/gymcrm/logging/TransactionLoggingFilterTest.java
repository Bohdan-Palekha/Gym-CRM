package com.gymcrm.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TransactionLoggingFilterTest {

    private final TransactionLoggingFilter filter = new TransactionLoggingFilter();

    @AfterEach
    void clearMdc() {
        // defense for a failing assertion mid-test, it shouldn't leak into other tests
        MDC.clear();
    }

    @Test
    @DisplayName("generates a transaction id and echoes it back as a response header")
    void generatesTransactionId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String header = response.getHeader("X-Transaction-Id");
        assertNotNull(header);
        assertFalse(header.isBlank());
    }

    @Test
    @DisplayName("reuses an incoming X-Transaction-Id header instead of generating a new one")
    void reusesIncomingTransactionId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");
        request.addHeader("X-Transaction-Id", "caller-supplied-id-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertEquals("caller-supplied-id-123", response.getHeader("X-Transaction-Id"));
    }

    @Test
    @DisplayName("the transaction id is present in MDC while the chain executes")
    void transactionIdAvailableDuringChainExecution() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcValueDuringChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) ->
                mdcValueDuringChain.set(MDC.get(TransactionLoggingFilter.TRANSACTION_ID_MDC_KEY)));

        assertNotNull(mdcValueDuringChain.get());
        assertEquals(response.getHeader("X-Transaction-Id"), mdcValueDuringChain.get());
    }

    @Test
    @DisplayName("MDC is cleared after the filter completes, even when the chain throws")
    void mdcClearedAfterCompletion_evenOnException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(IOException.class, () -> filter.doFilter(request, response, (req, res) -> {
            throw new IOException("downstream failure");
        }));

        assertNull(MDC.get(TransactionLoggingFilter.TRANSACTION_ID_MDC_KEY));
    }

    @Test
    @DisplayName("the response body written by the chain still reaches the real response")
    void responseBodyIsCopiedThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            res.setContentType("application/json");
            res.getWriter().write("[]");
        });

        assertEquals("[]", response.getContentAsString());
    }

    @Test
    @DisplayName("status code from the chain is preserved")
    void statusCodeIsPreserved() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainees/nobody");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> ((HttpServletResponse) res).setStatus(404));

        assertEquals(404, response.getStatus());
    }

    @Nested
    @DisplayName("sensitive query parameter masking")
    class QueryParameterMasking {

        private Logger logbackLogger;
        private ListAppender<ILoggingEvent> appender;

        @BeforeEach
        void attachAppender() {
            logbackLogger = (Logger) LoggerFactory.getLogger(TransactionLoggingFilter.class);
            appender = new ListAppender<>();
            appender.start();
            logbackLogger.addAppender(appender);
        }

        @AfterEach
        void detachAppender() {
            logbackLogger.detachAppender(appender);
        }

        @Test
        @DisplayName("redacts a password query parameter from the incoming request log line "
                + "(REST task 3 Login is a GET that carries the password as a query param)")
        void masksPasswordInQueryString() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
            request.setQueryString("username=alice.nguyen&password=superSecret123");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, (req, res) -> { });

            String logged = formattedMessages();
            assertFalse(logged.contains("superSecret123"), "password leaked into the log: " + logged);
            assertTrue(logged.contains("password=***"));
            assertTrue(logged.contains("username=alice.nguyen"), "non-sensitive params must not be touched");
        }

        @Test
        @DisplayName("redacts oldPassword and newPassword query parameters too (REST task 4 Change Login)")
        void masksChangePasswordParams() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/auth/change-password");
            request.setQueryString("oldPassword=oldSecret&newPassword=newSecret");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, (req, res) -> { });

            String logged = formattedMessages();
            assertFalse(logged.contains("oldSecret"), "old password leaked into the log: " + logged);
            assertFalse(logged.contains("newSecret"), "new password leaked into the log: " + logged);
        }

        private String formattedMessages() {
            return appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
        }
    }
}
