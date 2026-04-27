// src/main/java/it/univ/ddmanager/world/dto/WorldResponse.java
package it.univ.ddmanager.world.dto;

import it.univ.ddmanager.world.model.World;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorldResponse {

    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerNickname;
    private Long campaignCount;
    private Boolean isPublic;

    public static WorldResponse fromEntity(World world, Long campaignCount) {
        Long ownerId = world.getOwner() != null ? world.getOwner().getId() : null;
        String ownerNickname = world.getOwner() != null ? world.getOwner().getNickname() : null;
        return new WorldResponse(
            world.getId(),
            world.getName(),
            world.getDescription(),
            ownerId,
            ownerNickname,
            campaignCount != null ? campaignCount : 0L,
            world.getIsPublic()
        );
    }

    public static WorldResponse fromEntity(World world) {
        return fromEntity(world, null);
    }
}
