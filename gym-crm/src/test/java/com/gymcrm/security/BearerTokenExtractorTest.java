package com.gymcrm.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BearerTokenExtractor")
class BearerTokenExtractorTest {

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("extracts the token from a well-formed Bearer header")
    void wellFormedHeader_extractsToken() {
        when(request.getHeader("Authorization")).thenReturn("Bearer abc.def.ghi");
        assertEquals("abc.def.ghi", BearerTokenExtractor.extractToken(request));
    }

    @Test
    @DisplayName("no Authorization header returns null")
    void noHeader_returnsNull() {
        when(request.getHeader("Authorization")).thenReturn(null);
        assertNull(BearerTokenExtractor.extractToken(request));
    }

    @Test
    @DisplayName("a non-Bearer scheme (e.g. Basic) returns null")
    void nonBearerScheme_returnsNull() {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");
        assertNull(BearerTokenExtractor.extractToken(request));
    }

    @Test
    @DisplayName("an empty token after the Bearer prefix returns an empty string, not null")
    void emptyTokenAfterPrefix_returnsEmptyString() {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        assertEquals("", BearerTokenExtractor.extractToken(request));
    }
}
