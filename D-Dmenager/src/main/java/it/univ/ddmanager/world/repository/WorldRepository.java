// src/main/java/it/univ/ddmanager/world/repository/WorldRepository.java
package it.univ.ddmanager.world.repository;

import it.univ.ddmanager.world.model.World;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorldRepository extends JpaRepository<World, Long> {

    List<World> findByIsPublicTrue();

    List<World> findByOwner(it.univ.ddmanager.user.model.User owner);
}
