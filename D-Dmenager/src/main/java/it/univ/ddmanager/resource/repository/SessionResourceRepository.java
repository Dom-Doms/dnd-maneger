package it.univ.ddmanager.resource.repository;

import it.univ.ddmanager.campaign.model.Session;
import it.univ.ddmanager.resource.model.SessionResource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionResourceRepository extends JpaRepository<SessionResource, Long> {
    List<SessionResource> findBySessionOrderByUploadedAtDesc(Session session);
}
