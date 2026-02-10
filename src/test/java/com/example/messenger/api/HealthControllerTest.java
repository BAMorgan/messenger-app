package com.example.messenger.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {

    @Test
    void health_returnsOkStatusMap() {
        HealthController controller = new HealthController();

        Map<String, String> health = controller.health();

        assertEquals(Map.of("status", "ok"), health);
    }
}
