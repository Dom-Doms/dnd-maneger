// src/main/java/it/univ/ddmanager/location/dto/LocationResponse.java
package it.univ.ddmanager.location.dto;

import it.univ.ddmanager.location.model.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponse {

    private Long id;
    private Long worldId;
    private Long ownerId;
    private String ownerNickname;
    private Long parentLocationId;
    private String name;
    private String type;
    private String description;
    private String gmNotes;
    private Boolean isVisibleToPlayers;

    public static LocationResponse fromEntityForGm(Location location) {
        return new LocationResponse(
            location.getId(),
            location.getWorld() != null ? location.getWorld().getId() : null,
            location.getOwner() != null ? location.getOwner().getId() : null,
            location.getOwner() != null ? location.getOwner().getNickname() : null,
            location.getParentLocation() != null ? location.getParentLocation().getId() : null,
            location.getName(),
            location.getType(),
            location.getDescription(),
            location.getGmNotes(),
            location.isVisibleToPlayers()
        );
    }

    public static LocationResponse fromEntityForPlayer(Location location) {
        return new LocationResponse(
            location.getId(),
            location.getWorld() != null ? location.getWorld().getId() : null,
            location.getOwner() != null ? location.getOwner().getId() : null,
            location.getOwner() != null ? location.getOwner().getNickname() : null,
            location.getParentLocation() != null ? location.getParentLocation().getId() : null,
            location.getName(),
            location.getType(),
            location.getDescription(),
            null,
            location.isVisibleToPlayers()
        );
    }
}
