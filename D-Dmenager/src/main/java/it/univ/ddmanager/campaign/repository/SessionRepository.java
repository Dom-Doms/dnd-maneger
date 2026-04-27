// src/main/java/it/univ/ddmanager/campaign/repository/SessionRepository.java
package it.univ.ddmanager.campaign.repository;

import it.univ.ddmanager.campaign.model.Session;
import it.univ.ddmanager.user.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByCampaignId(Long campaignId);

    List<Session> findByOwner(User owner);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Session s WHERE s.owner.id = :userId OR s.campaign.id IN (SELECT cp.campaign.id FROM CampaignPlayer cp WHERE cp.player.id = :userId AND cp.status = 'APPROVED')")
    List<Session> findSessionsForUser(@org.springframework.data.repository.query.Param("userId") Long userId);
}
