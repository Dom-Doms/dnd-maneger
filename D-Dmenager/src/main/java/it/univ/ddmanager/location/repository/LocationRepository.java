// src/main/java/it/univ/ddmanager/location/repository/LocationRepository.java
package it.univ.ddmanager.location.repository;

import it.univ.ddmanager.location.model.Location;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByWorldId(Long worldId);

    List<Location> findByWorldIdAndIsVisibleToPlayersTrue(Long worldId);

    List<Location> findByIsVisibleToPlayersTrue();
}
