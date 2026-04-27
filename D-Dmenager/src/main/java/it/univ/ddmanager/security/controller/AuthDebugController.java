// src/main/java/it/univ/ddmanager/security/controller/AuthDebugController.java
package it.univ.ddmanager.security.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
public class AuthDebugController {

    // Utenti creati prima dell'introduzione di BCrypt avevano password in chiaro e non potranno autenticarsi.
    @GetMapping("/me")
    public Map<String, Object> me(Principal principal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
        return Map.of(
            "username", principal != null ? principal.getName() : null,
            "authorities", roles
        );
    }
}
