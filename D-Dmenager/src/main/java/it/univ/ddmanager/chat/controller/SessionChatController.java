package it.univ.ddmanager.chat.controller;

import it.univ.ddmanager.chat.dto.SessionChatMessageRequest;
import it.univ.ddmanager.chat.dto.SessionChatMessageResponse;
import it.univ.ddmanager.chat.service.SessionChatService;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.service.CurrentUserService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/chat/messages")
@RequiredArgsConstructor
public class SessionChatController {

    private final SessionChatService sessionChatService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<SessionChatMessageResponse> sendMessage(@PathVariable Long sessionId,
            @Valid @RequestBody SessionChatMessageRequest request) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        SessionChatMessageResponse response = sessionChatService.sendMessage(sessionId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<SessionChatMessageResponse> listMessages(@PathVariable Long sessionId,
            @RequestParam(value = "recipientUserId", required = false) Long recipientUserId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        return sessionChatService.listMessages(sessionId, currentUser, recipientUserId, from);
    }
}
