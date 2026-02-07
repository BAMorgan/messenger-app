package com.example.messenger.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configures distributed tracing via OpenTelemetry.
 *
 * <p>Tracing is auto-configured by Spring Boot Actuator + Micrometer Tracing.
 * The OpenTelemetry bridge ({@code micrometer-tracing-bridge-otel}) and OTLP exporter
 * ({@code opentelemetry-exporter-otlp}) are on the classpath.
 *
 * <p>Key properties (in {@code application.yml}):
 * <ul>
 *   <li>{@code management.tracing.sampling.probability} — sampling rate (0.0–1.0)</li>
 *   <li>{@code management.otlp.tracing.endpoint} — OTLP collector endpoint (optional)</li>
 * </ul>
 *
 * <p>When no OTLP endpoint is configured, traces are still created and propagated
 * (trace IDs appear in logs via MDC) but are not exported to a remote collector.
 *
 * <p>To export traces to an OpenTelemetry collector, add to {@code application.yml}:
 * <pre>
 *   management:
 *     otlp:
 *       tracing:
 *         endpoint: http://otel-collector:4318/v1/traces
 * </pre>
 */
@Configuration
public class TracingConfig {
    // Auto-configuration handles OpenTelemetry setup.
    // Custom span processors, exporters, or propagators can be added here as @Bean methods.
}
