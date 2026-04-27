package it.univ.ddmanager.support;

import it.univ.ddmanager.security.JwtTokenService;
import it.univ.ddmanager.security.UserPrincipal;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@Component
@RequiredArgsConstructor
public class TestAuthenticationHelper {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    public RequestPostProcessor bearerToken(String email) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(email));
            return request;
        };
    }

    public RequestPostProcessor bearerToken(String email, String ignoredPassword) {
        return bearerToken(email);
    }

    public String generateToken(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Test user not found: " + email));
        UserPrincipal principal = UserPrincipal.fromUser(user);
        return jwtTokenService.generateToken(principal);
    }
}
