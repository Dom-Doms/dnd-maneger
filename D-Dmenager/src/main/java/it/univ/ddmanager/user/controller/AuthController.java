// src/main/java/it/univ/ddmanager/user/controller/AuthController.java
package it.univ.ddmanager.user.controller;

import it.univ.ddmanager.common.InvalidCredentialsException;
import it.univ.ddmanager.security.JwtTokenService;
import it.univ.ddmanager.security.UserPrincipal;
import it.univ.ddmanager.user.dto.AuthResponse;
import it.univ.ddmanager.user.dto.LoginRequest;
import it.univ.ddmanager.user.dto.RegisterRequest;
import it.univ.ddmanager.user.dto.UserResponse;
import it.univ.ddmanager.user.dto.UserUpdateRequest;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.service.CurrentUserService;
import it.univ.ddmanager.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request);
        UserResponse response = UserResponse.fromEntity(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        userService.getByEmailOrThrow(request.getEmail());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtTokenService.generateToken(principal);
        return ResponseEntity.ok(AuthResponse.of(token, UserResponse.fromPrincipal(principal)));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        User user = currentUserService.getCurrentUserOrThrow();
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@RequestBody UserUpdateRequest request) {
        User current = currentUserService.getCurrentUserOrThrow();
        User updated = userService.updateCurrentUser(current, request);
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }
}
