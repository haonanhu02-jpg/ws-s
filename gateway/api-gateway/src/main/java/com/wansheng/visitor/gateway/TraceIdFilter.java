package com.wansheng.visitor.gateway;

import com.wansheng.visitor.common.core.TraceId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class TraceIdFilter extends OncePerRequestFilter {
    static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String traceId = incoming == null || incoming.isBlank() ? TraceId.create() : incoming;
        response.setHeader(HEADER, traceId);
        chain.doFilter(request, response);
    }
}

