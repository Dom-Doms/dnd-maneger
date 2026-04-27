// src/main/java/it/univ/ddmanager/campaign/controller/SessionController.java
package it.univ.ddmanager.campaign.controller;

import it.univ.ddmanager.campaign.dto.SessionRequest;
import it.univ.ddmanager.campaign.dto.SessionResponse;
import it.univ.ddmanager.campaign.model.Session;
import it.univ.ddmanager.campaign.service.SessionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PostMapping("/campaigns/{campaignId}/sessions")
    public ResponseEntity<SessionResponse> createSession(@PathVariable Long campaignId,
            @Valid @RequestBody SessionRequest request) {
        Session session = sessionService.createSession(campaignId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.fromEntity(session));
    }

    @GetMapping("/campaigns/{campaignId}/sessions")
    public List<SessionResponse> getSessionsByCampaign(@PathVariable Long campaignId) {
        return sessionService.getSessionsByCampaign(campaignId).stream()
                .map(SessionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM','PLAYER')")
    @GetMapping("/sessions/my")
    public List<SessionResponse> getMySessions() {
        return sessionService.getMySessions().stream()
                .map(SessionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/sessions/{sessionId}")
    public SessionResponse getSession(@PathVariable Long sessionId) {
        Session session = sessionService.getSessionById(sessionId);
        return SessionResponse.fromEntity(session);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PutMapping("/sessions/{sessionId}")
    public SessionResponse updateSession(@PathVariable Long sessionId,
            @Valid @RequestBody SessionRequest request) {
        Session updated = sessionService.updateSession(sessionId, request);
        return SessionResponse.fromEntity(updated);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        sessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
