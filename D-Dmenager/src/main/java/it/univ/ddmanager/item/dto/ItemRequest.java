// src/main/java/it/univ/ddmanager/item/dto/ItemRequest.java
package it.univ.ddmanager.item.dto;

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
public class ItemRequest {

    @NotNull
    private Long worldId;

    private Long locationId;

    @NotBlank
    private String name;

    private String type;

    private String rarity;

    private String description;

    private String gmNotes;

    private Boolean isVisibleToPlayers;
}
