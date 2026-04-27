// src/main/java/it/univ/ddmanager/item/dto/ItemResponse.java
package it.univ.ddmanager.item.dto;

import it.univ.ddmanager.item.model.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemResponse {

    private Long id;
    private Long worldId;
    private Long locationId;
    private Long ownerId;
    private String ownerNickname;
    private String name;
    private String type;
    private String rarity;
    private String description;
    private String gmNotes;
    private Boolean isVisibleToPlayers;

    public static ItemResponse fromEntityForGm(Item item) {
        return new ItemResponse(
            item.getId(),
            item.getWorld() != null ? item.getWorld().getId() : null,
            item.getLocation() != null ? item.getLocation().getId() : null,
            item.getOwner() != null ? item.getOwner().getId() : null,
            item.getOwner() != null ? item.getOwner().getNickname() : null,
            item.getName(),
            item.getType(),
            item.getRarity(),
            item.getDescription(),
            item.getGmNotes(),
            item.isVisibleToPlayers()
        );
    }

    public static ItemResponse fromEntityForPlayer(Item item) {
        return new ItemResponse(
            item.getId(),
            item.getWorld() != null ? item.getWorld().getId() : null,
            item.getLocation() != null ? item.getLocation().getId() : null,
            item.getOwner() != null ? item.getOwner().getId() : null,
            item.getOwner() != null ? item.getOwner().getNickname() : null,
            item.getName(),
            item.getType(),
            item.getRarity(),
            item.getDescription(),
            null,
            item.isVisibleToPlayers()
        );
    }
}
