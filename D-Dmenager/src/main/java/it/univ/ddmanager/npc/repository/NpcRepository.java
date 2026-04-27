// src/main/java/it/univ/ddmanager/npc/repository/NpcRepository.java
package it.univ.ddmanager.npc.repository;

import it.univ.ddmanager.npc.model.Npc;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NpcRepository extends JpaRepository<Npc, Long> {

    List<Npc> findByWorldId(Long worldId);

    List<Npc> findByIsVisibleToPlayersTrue();

    List<Npc> findByWorldIdAndIsVisibleToPlayersTrue(Long worldId);
}
