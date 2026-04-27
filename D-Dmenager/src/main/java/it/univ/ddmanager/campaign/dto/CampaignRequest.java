// src/main/java/it/univ/ddmanager/campaign/dto/CampaignRequest.java
package it.univ.ddmanager.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRequest {

    @NotNull
    private Long worldId;

    @NotBlank
    private String name;

    private String description;

    private String status;
}