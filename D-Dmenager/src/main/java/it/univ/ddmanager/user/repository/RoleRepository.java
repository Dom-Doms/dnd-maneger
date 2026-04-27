// src/main/java/it/univ/ddmanager/user/repository/RoleRepository.java
package it.univ.ddmanager.user.repository;

import it.univ.ddmanager.user.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);
}
