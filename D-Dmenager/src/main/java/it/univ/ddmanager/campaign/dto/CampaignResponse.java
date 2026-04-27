// src/main/java/it/univ/ddmanager/campaign/dto/CampaignResponse.java
package it.univ.ddmanager.campaign.dto;

import it.univ.ddmanager.campaign.model.Campaign;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {

    private Long id;
    private String name;
    private String description;
    private String status;
    private Long worldId;
    private Long ownerId;
    private String ownerNickname;

    public static CampaignResponse fromEntity(Campaign campaign) {
        Long ownerId = campaign.getOwner() != null ? campaign.getOwner().getId() : null;
        String ownerNickname = campaign.getOwner() != null ? campaign.getOwner().getNickname() : null;
        return new CampaignResponse(
            campaign.getId(),
            campaign.getName(),
            campaign.getDescription(),
            campaign.getStatus() != null ? campaign.getStatus().name() : null,
            campaign.getWorld() != null ? campaign.getWorld().getId() : null,
            ownerId,
            ownerNickname
        );
    }
}
