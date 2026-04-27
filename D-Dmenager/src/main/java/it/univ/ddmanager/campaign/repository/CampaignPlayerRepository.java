package it.univ.ddmanager.campaign.repository;

import it.univ.ddmanager.campaign.model.Campaign;
import it.univ.ddmanager.campaign.model.CampaignPlayer;
import it.univ.ddmanager.campaign.model.CampaignPlayerStatus;
import it.univ.ddmanager.character.model.PlayerCharacter;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.world.model.World;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignPlayerRepository extends JpaRepository<CampaignPlayer, Long> {

    List<CampaignPlayer> findByCampaign(Campaign campaign);

    List<CampaignPlayer> findByCampaignAndStatus(Campaign campaign, CampaignPlayerStatus status);

    List<CampaignPlayer> findByPlayer(User player);

    List<CampaignPlayer> findByPlayerAndStatus(User player, CampaignPlayerStatus status);

    Optional<CampaignPlayer> findByIdAndCampaign(Long id, Campaign campaign);

    Optional<CampaignPlayer> findByCampaignAndPlayer(Campaign campaign, User player);

    Optional<CampaignPlayer> findByCampaignAndPlayerAndStatus(
        Campaign campaign,
        User player,
        CampaignPlayerStatus status
    );

    Optional<CampaignPlayer> findByCampaignAndCharacterAndStatus(
        Campaign campaign,
        PlayerCharacter character,
        CampaignPlayerStatus status
    );

    boolean existsByCampaign_WorldAndPlayerAndStatus(
        World world,
        User player,
        CampaignPlayerStatus status
    );

    boolean existsByCampaignAndCharacterAndStatusIn(
        Campaign campaign,
        PlayerCharacter character,
        Collection<CampaignPlayerStatus> statuses
    );

    List<CampaignPlayer> findByStatus(CampaignPlayerStatus status);

    List<CampaignPlayer> findByCampaignOwnerIdAndStatus(Long ownerId, CampaignPlayerStatus status);
}
