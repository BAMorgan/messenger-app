package com.example.messenger.config;

import com.example.messenger.service.EventService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Micrometer metrics for the Messenger application.
 *
 * <p>Common tags (application name) are applied via {@code management.metrics.tags}
 * in {@code application.yml}.
 *
 * <p>Custom metrics registered:
 * <ul>
 *   <li>{@code messenger.messages.sent} (counter) — total messages sent, recorded by {@code MessageService}</li>
 *   <li>{@code messenger.messages.send.duration} (timer) — message send latency, recorded by {@code MessageService}</li>
 *   <li>{@code messenger.websocket.connections} (gauge) — active WebSocket connections, read from {@code EventService}</li>
 *   <li>{@code messenger.events.published} (counter) — total events published, recorded by {@code EventService}</li>
 * </ul>
 */
@Configuration
public class MetricsConfig {

    /**
     * Registers a gauge that tracks the number of active WebSocket connections.
     * The gauge reads from {@link EventService#getActiveConnectionCount()} on each scrape.
     */
    @Bean
    public MeterBinder webSocketConnectionsMetrics(EventService eventService) {
        return registry -> Gauge.builder("messenger.websocket.connections",
                        eventService, EventService::getActiveConnectionCount)
                .description("Number of active WebSocket connections")
                .register(registry);
    }
}
