// src/main/java/it/univ/ddmanager/user/repository/UserRepository.java
package it.univ.ddmanager.user.repository;

import it.univ.ddmanager.user.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}