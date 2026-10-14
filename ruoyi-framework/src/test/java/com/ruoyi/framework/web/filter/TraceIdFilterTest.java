package com.ruoyi.framework.web.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import com.ruoyi.common.filter.TraceIdFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceIdFilterTest
{
    private static final String VALID_TRACE_ID = "87c4db2f6b574ad6aafe6b715741d408";

    @AfterEach
    void clearMdc()
    {
        MDC.clear();
    }

    @Test
    void echoesValidHeaderAndClearsMdcAfterward() throws Exception
    {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, VALID_TRACE_ID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertEquals(VALID_TRACE_ID, MDC.get(TraceIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertEquals(VALID_TRACE_ID, response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertNull(MDC.get(TraceIdFilter.MDC_KEY));
    }

    @Test
    void readsHeaderCaseInsensitively() throws Exception
    {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-trace-id", VALID_TRACE_ID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertEquals(VALID_TRACE_ID, MDC.get(TraceIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertEquals(VALID_TRACE_ID, response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
    }

    @Test
    void generatesHexIdWhenHeaderMissing() throws Exception
    {
        assertGeneratedTraceId(new MockHttpServletRequest());
    }

    @Test
    void generatesHexIdWhenHeaderInvalid() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "not a valid id!!");
        String generated = assertGeneratedTraceId(request);
        assertTrue(!generated.equals("not a valid id!!"));
    }

    private String assertGeneratedTraceId(MockHttpServletRequest request) throws Exception
    {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String[] seen = new String[1];
        FilterChain chain = (req, res) -> {
            seen[0] = MDC.get(TraceIdFilter.MDC_KEY);
            assertTrue(seen[0] != null && seen[0].matches("^[a-fA-F0-9]{32}$"));
        };

        filter.doFilter(request, response, chain);

        assertEquals(seen[0], response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertNull(MDC.get(TraceIdFilter.MDC_KEY));
        return seen[0];
    }
}
