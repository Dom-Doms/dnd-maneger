// src/main/java/it/univ/ddmanager/user/service/CurrentUserService.java
package it.univ.ddmanager.user.service;

import it.univ.ddmanager.common.UnauthorizedException;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymousPrincipal = authentication != null
            && authentication.getPrincipal() instanceof String
            && "anonymousUser".equals(authentication.getPrincipal());
        if (authentication == null || !authentication.isAuthenticated() || anonymousPrincipal) {
            throw new UnauthorizedException("No authenticated user present");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("User not found: " + email));
    }
}
