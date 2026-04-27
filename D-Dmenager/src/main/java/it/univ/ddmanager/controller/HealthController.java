// src/main/java/it/univ/ddmanager/controller/HealthController.java
package it.univ.ddmanager.controller;

import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("OK", OffsetDateTime.now());
    }

    private record HealthResponse(String status, OffsetDateTime timestamp) {
    }
}
