package com.example.messenger.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController//ready to handle HTTP requests // return values become JSON automatically
public class HealthController {

    @GetMapping("/health")
    public Map<String,String> health(){
        return Map.of("status","ok");
    }
}
