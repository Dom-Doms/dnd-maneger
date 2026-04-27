// src/main/java/it/univ/ddmanager/sessionlog/controller/SessionEventController.java
package it.univ.ddmanager.sessionlog.controller;

import it.univ.ddmanager.sessionlog.dto.SessionEventRequest;
import it.univ.ddmanager.sessionlog.dto.SessionEventResponse;
import it.univ.ddmanager.sessionlog.model.SessionEvent;
import it.univ.ddmanager.sessionlog.service.SessionEventService;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
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
public class SessionEventController {

    private final SessionEventService sessionEventService;

    @GetMapping("/sessions/{sessionId}/events")
    public List<SessionEventResponse> getEventsBySession(@PathVariable Long sessionId) {
        User currentUser = sessionEventService.getCurrentUser();
        List<SessionEvent> events = sessionEventService.getEventsForUserBySession(sessionId, currentUser);
        boolean gmOrAdmin = isGmOrAdmin(currentUser);
        return events.stream()
            .map(event -> gmOrAdmin ? SessionEventResponse.fromEntityForGm(event) : SessionEventResponse.fromEntityForPlayer(event))
            .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PostMapping("/session-events")
    public ResponseEntity<SessionEventResponse> create(@Valid @RequestBody SessionEventRequest request) {
        SessionEvent event = sessionEventService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionEventResponse.fromEntityForGm(event));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PutMapping("/session-events/{id}")
    public SessionEventResponse update(@PathVariable Long id, @Valid @RequestBody SessionEventRequest request) {
        SessionEvent event = sessionEventService.update(id, request);
        return SessionEventResponse.fromEntityForGm(event);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @DeleteMapping("/session-events/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sessionEventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isGmOrAdmin(User user) {
        return user.getRoles().stream()
            .map(Role::getName)
            .anyMatch(role -> "ROLE_GM".equals(role) || "ROLE_ADMIN".equals(role));
    }
}
