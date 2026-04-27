package it.univ.ddmanager.campaign.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignPlayerRequest {

    @NotNull
    private Long characterId;

    @Size(max = 1000)
    private String message;
}
