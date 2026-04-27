// src/main/java/it/univ/ddmanager/sessionlog/repository/SessionEventRepository.java
package it.univ.ddmanager.sessionlog.repository;

import it.univ.ddmanager.sessionlog.model.SessionEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionEventRepository extends JpaRepository<SessionEvent, Long> {

    List<SessionEvent> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    List<SessionEvent> findTop10ByOrderByCreatedAtDesc();

    List<SessionEvent> findTop10ByIsVisibleToPlayersTrueOrderByCreatedAtDesc();

    long countByIsVisibleToPlayersTrue();
}
