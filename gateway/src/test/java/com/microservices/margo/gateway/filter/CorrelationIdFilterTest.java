package com.microservices.margo.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock FilterChain chain;

    @InjectMocks CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "correlationIdHeader", "X-Correlation-Id");
        ReflectionTestUtils.setField(filter, "mdcKey", "correlationId");
    }

    @AfterEach
    void tearDown() { MDC.clear(); }

    @Test
    void passesExistingCorrelationId_throughToChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "existing-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("existing-id");
        verify(chain).doFilter(any(), any());
    }

    @Test
    void generatesId_andSetsResponseHeader_whenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-Correlation-Id")).isNotBlank();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void generatesId_whenHeaderIsBlank() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        // generated id must not be blank
        assertThat(response.getHeader("X-Correlation-Id")).isNotBlank();
    }

    @Test
    void mutatedRequest_returnsInjectedHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Capture the mutated request wrapper passed to the chain
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        doNothing().when(chain).doFilter(requestCaptor.capture(), any());

        filter.doFilterInternal(request, response, chain);

        HttpServletRequest mutated = requestCaptor.getValue();
        // The mutated wrapper must expose the correlation id via getHeader
        assertThat(mutated.getHeader("X-Correlation-Id")).isNotBlank();
        // And enumeration must contain it
        assertThat(mutated.getHeaders("X-Correlation-Id").hasMoreElements()).isTrue();
    }

    @Test
    void clearsMdc_afterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "clear-test");

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void clearsMdc_evenWhenChainThrows() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new RuntimeException("boom")).when(chain).doFilter(any(), any());

        try {
            filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        } catch (RuntimeException ignored) {}

        assertThat(MDC.get("correlationId")).isNull();
    }
}