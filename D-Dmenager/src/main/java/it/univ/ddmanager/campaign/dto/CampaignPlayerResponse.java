package it.univ.ddmanager.campaign.dto;

import it.univ.ddmanager.campaign.model.Campaign;
import it.univ.ddmanager.campaign.model.CampaignPlayer;
import it.univ.ddmanager.campaign.model.CampaignPlayerStatus;
import it.univ.ddmanager.character.model.PlayerCharacter;
import it.univ.ddmanager.user.model.User;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignPlayerResponse {

    private Long id;
    private Long campaignId;
    private String campaignName;
    private Long worldId;
    private Long characterId;
    private String characterName;
    private Integer characterLevel;
    private String characterClass;
    private String characterSubclass;
    private Long playerId;
    private String playerNickname;
    private CampaignPlayerStatus status;
    private String message;
    private Long decisionById;
    private String decisionByNickname;
    private Instant createdAt;
    private Instant updatedAt;
    private java.util.Set<String> knownLanguages;

    public static CampaignPlayerResponse fromEntity(CampaignPlayer campaignPlayer) {
        Campaign campaign = campaignPlayer.getCampaign();
        PlayerCharacter character = campaignPlayer.getCharacter();
        User decisionBy = campaignPlayer.getDecisionBy();
        return new CampaignPlayerResponse(
                campaignPlayer.getId(),
                campaign != null ? campaign.getId() : null,
                campaign != null ? campaign.getName() : null,
                campaign != null && campaign.getWorld() != null
                        ? campaign.getWorld().getId()
                        : null,
                character != null ? character.getId() : null,
                character != null ? character.getName() : null,
                character != null ? character.getLevel() : null,
                character != null ? character.getCharacterClass() : null,
                character != null ? character.getSubclass() : null,
                campaignPlayer.getPlayer() != null ? campaignPlayer.getPlayer().getId() : null,
                campaignPlayer.getPlayer() != null ? campaignPlayer.getPlayer().getNickname() : null,
                campaignPlayer.getStatus(),
                campaignPlayer.getMessage(),
                decisionBy != null ? decisionBy.getId() : null,
                decisionBy != null ? decisionBy.getNickname() : null,
                campaignPlayer.getCreatedAt(),
                campaignPlayer.getUpdatedAt(),
                character != null && character.getKnownLanguages() != null
                        ? character.getKnownLanguages().stream().map(Enum::name)
                                .collect(java.util.stream.Collectors.toSet())
                        : java.util.Collections.emptySet());
    }
}
