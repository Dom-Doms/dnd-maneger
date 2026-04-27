// src/main/java/it/univ/ddmanager/sessionlog/service/SessionEventService.java
package it.univ.ddmanager.sessionlog.service;

import it.univ.ddmanager.campaign.model.Session;
import it.univ.ddmanager.campaign.repository.SessionRepository;
import it.univ.ddmanager.common.ResourceNotFoundException;
import it.univ.ddmanager.sessionlog.dto.SessionEventRequest;
import it.univ.ddmanager.sessionlog.model.SessionEvent;
import it.univ.ddmanager.sessionlog.repository.SessionEventRepository;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.service.CurrentUserService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionEventService {

    private final SessionEventRepository sessionEventRepository;
    private final SessionRepository sessionRepository;
    private final CurrentUserService currentUserService;

    public List<SessionEvent> getEventsForUserBySession(Long sessionId, User currentUser) {
        Session session = sessionRepository.findById(Objects.requireNonNull(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id " + sessionId));
        boolean gmOrAdmin = isGmOrAdmin(currentUser);
        List<SessionEvent> events = sessionEventRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        if (gmOrAdmin) {
            return events;
        }
        return events.stream()
                .filter(SessionEvent::isVisibleToPlayers)
                .collect(Collectors.toList());
    }

    public SessionEvent create(SessionEventRequest request) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requireGmOrAdmin(currentUser);
        Session session = sessionRepository.findById(Objects.requireNonNull(request.getSessionId()))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Session not found with id " + request.getSessionId()));

        SessionEvent event = new SessionEvent();
        event.setSession(session);
        event.setOwner(currentUser);
        event.setTitle(request.getTitle());
        event.setType(request.getType());
        event.setDescription(request.getDescription());
        event.setInGameTime(request.getInGameTime());
        event.setVisibleToPlayers(request.getIsVisibleToPlayers() == null || request.getIsVisibleToPlayers());
        event.setCreatedAt(Instant.now());
        return sessionEventRepository.save(event);
    }

    public SessionEvent update(Long id, SessionEventRequest request) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requireGmOrAdmin(currentUser);
        SessionEvent event = sessionEventRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Session event not found with id " + id));

        event.setTitle(request.getTitle());
        event.setType(request.getType());
        event.setDescription(request.getDescription());
        event.setInGameTime(request.getInGameTime());
        event.setVisibleToPlayers(request.getIsVisibleToPlayers() == null || request.getIsVisibleToPlayers());
        return sessionEventRepository.save(event);
    }

    public void delete(Long id) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requireGmOrAdmin(currentUser);
        SessionEvent event = sessionEventRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Session event not found with id " + id));
        sessionEventRepository.delete(Objects.requireNonNull(event));
    }

    public User getCurrentUser() {
        return currentUserService.getCurrentUserOrThrow();
    }

    private void requireGmOrAdmin(User currentUser) {
        if (!isGmOrAdmin(currentUser)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private boolean isGmOrAdmin(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(role -> "ROLE_GM".equals(role) || "ROLE_ADMIN".equals(role));
    }
}
