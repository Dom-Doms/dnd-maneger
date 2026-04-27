// src/main/java/it/univ/ddmanager/user/service/UserService.java
package it.univ.ddmanager.user.service;

import it.univ.ddmanager.common.EmailAlreadyUsedException;
import it.univ.ddmanager.common.ResourceNotFoundException;
import it.univ.ddmanager.common.InvalidRoleException;
import it.univ.ddmanager.user.dto.RegisterRequest;
import it.univ.ddmanager.user.dto.UserUpdateRequest;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.repository.RoleRepository;
import it.univ.ddmanager.user.repository.UserRepository;
import java.util.Set;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(RegisterRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new EmailAlreadyUsedException("Email already in use");
        });

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRoles(resolveRolesForRegistration(request.getRole()));

        return userRepository.save(Objects.requireNonNull(user));
    }

    private Set<Role> resolveRolesForRegistration(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            throw new InvalidRoleException("Invalid role: " + requestedRole);
        }
        String normalized = requestedRole.trim().toUpperCase();
        return switch (normalized) {
            case "PLAYER" -> Set.of(loadRole("ROLE_PLAYER"));
            case "DM" -> Set.of(loadRole("ROLE_GM"));
            case "VIEWER" -> Set.of(loadRole("ROLE_VIEWER"));
            default -> throw new InvalidRoleException("Invalid role: " + requestedRole);
        };
    }

    private Role loadRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Default role " + roleName + " not found"));
    }

    public User updateCurrentUser(User currentUser, UserUpdateRequest request) {
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            currentUser.setNickname(request.getNickname());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            currentUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return userRepository.save(Objects.requireNonNull(currentUser));
    }

    public User getByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
