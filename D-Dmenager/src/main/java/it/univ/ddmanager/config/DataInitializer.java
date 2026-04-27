// src/main/java/it/univ/ddmanager/config/DataInitializer.java
package it.univ.ddmanager.config;

import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.repository.RoleRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Bean
    CommandLineRunner seedDefaultRoles() {
        return args -> {
            List<String> defaultRoles = List.of(
                "ROLE_ADMIN",
                "ROLE_GM",
                "ROLE_PLAYER",
                "ROLE_VIEWER"
            );
            defaultRoles.forEach(roleName ->
                roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(new Role(null, roleName)))
            );
        };
    }
}
