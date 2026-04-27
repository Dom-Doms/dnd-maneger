// src/main/java/it/univ/ddmanager/campaign/repository/CampaignRepository.java
package it.univ.ddmanager.campaign.repository;

import it.univ.ddmanager.campaign.model.Campaign;
import it.univ.ddmanager.user.model.User;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByWorldId(Long worldId);

    List<Campaign> findByWorldIdIn(Collection<Long> worldIds);

    long countByWorldId(Long worldId);

    boolean existsByWorldIdAndNameIgnoreCase(Long worldId, String name);

    boolean existsByWorldIdAndNameIgnoreCaseAndIdNot(Long worldId, String name, Long id);

    List<Campaign> findByOwner(User owner);
}
