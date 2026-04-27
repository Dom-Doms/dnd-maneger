package it.univ.ddmanager.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtTokenService {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expirationSeconds;

    public JwtTokenService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(this.algorithm).build();
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(UserPrincipal user) {
        Instant now = Instant.now();
        List<String> roles = user.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .collect(Collectors.toList());
        return JWT.create()
            .withSubject(user.getUsername())
            .withClaim("userId", user.getId())
            .withClaim("email", user.getUsername())
            .withClaim("nickname", user.getNickname())
            .withClaim("roles", roles)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(expirationSeconds)))
            .sign(algorithm);
    }

    public String getUsernameFromToken(String token) {
        return verify(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            verify(token);
            return true;
        } catch (JWTVerificationException ex) {
            log.debug("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    private DecodedJWT verify(String token) {
        return verifier.verify(token);
    }
}
