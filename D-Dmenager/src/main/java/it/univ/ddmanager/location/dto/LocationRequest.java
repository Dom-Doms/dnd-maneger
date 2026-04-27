// src/main/java/it/univ/ddmanager/location/dto/LocationRequest.java
package it.univ.ddmanager.location.dto;

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
public class LocationRequest {

    @NotNull
    private Long worldId;

    private Long parentLocationId;

    @NotBlank
    private String name;

    private String type;
    private String description;
    private String gmNotes;
    private Boolean isVisibleToPlayers;
}
