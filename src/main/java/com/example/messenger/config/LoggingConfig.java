package com.example.messenger.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Configures structured logging and correlation ID propagation.
 *
 * <p>Every HTTP request gets a unique {@code requestId} added to the MDC (Mapped Diagnostic
 * Context), which appears in all log output during that request. If the caller supplies an
 * {@code X-Request-ID} header, that value is reused; otherwise a UUID is generated.
 *
 * <p>The log pattern in {@code application.yml} includes {@code requestId}, {@code traceId},
 * and {@code spanId} (the latter two are populated automatically by Micrometer Tracing).
 *
 * <p>For JSON structured logging in production, activate with:
 * <pre>
 *   logging.structured.format.console=ecs
 * </pre>
 * or provide a custom {@code logback-spring.xml} with {@code JsonEncoder}.
 */
@Configuration
public class LoggingConfig {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_REQUEST_ID = "requestId";

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorrelationIdFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("correlationIdFilter");
        return registration;
    }

    /**
     * Servlet filter that adds a correlation/request ID to MDC for every request.
     * The request ID is also returned as a response header for client-side correlation.
     */
    static class CorrelationIdFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            MDC.put(MDC_REQUEST_ID, requestId);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            try {
                filterChain.doFilter(request, response);
            } finally {
                MDC.remove(MDC_REQUEST_ID);
            }
        }
    }
}
