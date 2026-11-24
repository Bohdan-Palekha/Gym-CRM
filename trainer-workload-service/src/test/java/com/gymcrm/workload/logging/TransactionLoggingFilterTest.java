package com.gymcrm.workload.logging;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransactionLoggingFilter — unit tests")
class TransactionLoggingFilterTest {

    private final TransactionLoggingFilter filter = new TransactionLoggingFilter();

    @AfterEach
    void clearMdc() {
        // defense for a failing assertion mid-test
        MDC.clear();
    }

    @Test
    @DisplayName("generates a transaction id and echoes it back as a response header")
    void generatesTransactionId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/trainer-workloads");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String header = response.getHeader("X-Transaction-Id");
        assertNotNull(header);
        assertFalse(header.isBlank());
    }

    @Test
    @DisplayName("reuses an incoming X-Transaction-Id header instead of generating a new one "
            + "(task 8: the id gym-crm forwards ties both services' logs together)")
    void reusesIncomingTransactionId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/trainer-workloads");
        request.addHeader("X-Transaction-Id", "caller-supplied-id-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertEquals("caller-supplied-id-123", response.getHeader("X-Transaction-Id"));
    }

    @Test
    @DisplayName("a blank incoming header is treated as absent, not reused verbatim")
    void blankIncomingHeader_generatesNewId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/trainer-workloads");
        request.addHeader("X-Transaction-Id", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String header = response.getHeader("X-Transaction-Id");
        assertNotNull(header);
        assertFalse(header.isBlank(), "a blank incoming header must not be reused verbatim");
        assertNotEquals("   ", header);
    }

    @Test
    @DisplayName("the transaction id is present in MDC while the chain executes")
    void transactionIdAvailableDuringChainExecution() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainer-workloads/bob.trainer");
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
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/trainer-workloads");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(IOException.class, () -> filter.doFilter(request, response, (req, res) -> {
            throw new IOException("downstream failure");
        }));

        assertNull(MDC.get(TransactionLoggingFilter.TRANSACTION_ID_MDC_KEY));
    }

    @Test
    @DisplayName("the response body written by the chain still reaches the real response")
    void responseBodyIsCopiedThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainer-workloads/bob.trainer");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            res.setContentType("application/json");
            res.getWriter().write("{\"trainerUsername\":\"bob.trainer\"}");
        });

        assertEquals("{\"trainerUsername\":\"bob.trainer\"}", response.getContentAsString());
    }

    @Test
    @DisplayName("status code from the chain is preserved")
    void statusCodeIsPreserved() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trainer-workloads/nobody");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> ((HttpServletResponse) res).setStatus(404));

        assertEquals(404, response.getStatus());
    }

    @Test
    @DisplayName("an empty request/response body does not break logging")
    void emptyBodies_doNotThrow() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/trainer-workloads");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertDoesNotThrow(() -> filter.doFilter(request, response, (req, res) -> { }));
    }
}
