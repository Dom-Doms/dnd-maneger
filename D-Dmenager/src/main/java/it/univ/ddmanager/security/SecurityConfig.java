// src/main/java/it/univ/ddmanager/security/SecurityConfig.java
package it.univ.ddmanager.security;

import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            return UserPrincipal.fromUser(user);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/dice/d20").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/dice/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/characters/**").hasAnyRole("ADMIN", "GM", "PLAYER")
                .requestMatchers(HttpMethod.PUT, "/api/characters/**").hasAnyRole("ADMIN", "GM", "PLAYER")
                .requestMatchers(HttpMethod.PATCH, "/api/characters/**").hasAnyRole("ADMIN", "GM", "PLAYER")
                .requestMatchers(HttpMethod.DELETE, "/api/characters/**").hasAnyRole("ADMIN", "GM", "PLAYER")
                .requestMatchers(HttpMethod.POST, "/api/campaigns/*/join-requests").hasAnyRole("ADMIN", "GM", "PLAYER")
                .requestMatchers(HttpMethod.POST, "/api/campaigns/*/join-requests/*/approve").hasAnyRole("ADMIN", "GM")
                //.requestMatchers(HttpMethod.GET, "/api/campaigns/*/sessions").hasAnyRole("ADMIN", "GM")
                .requestMatchers(HttpMethod.POST, "/api/campaigns/*/join-requests/*/reject").hasAnyRole("ADMIN", "GM")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenService jwtTokenService, UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtTokenService, userDetailsService);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
