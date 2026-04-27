package it.univ.ddmanager.character.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHitPointsRequest {

    @Min(0)
    private Integer currentHitPoints;
}
