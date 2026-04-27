// src/main/java/it/univ/ddmanager/npc/dto/NpcRequest.java
package it.univ.ddmanager.npc.dto;

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
public class NpcRequest {

    @NotNull
    private Long worldId;

    @NotBlank
    private String name;

    private String race;
    private String roleOrClass;
    private String description;
    private String gmNotes;
    private Boolean isVisibleToPlayers;
    private String alignment;
    private String size;
    private String creatureType;
    private Integer armorClass;
    private Integer maxHitPoints;
    private Integer currentHitPoints;
    private Integer temporaryHitPoints;
    private String hitDice;
    private String speed;
    private Integer strength;
    private Integer dexterity;
    private Integer constitution;
    private Integer intelligence;
    private Integer wisdom;
    private Integer charisma;
    private String savingThrows;
    private String skills;
    private String damageResistances;
    private String damageImmunities;
    private String conditionImmunities;
    private String senses;
    private String languages;
    private String challengeRating;
    private Integer experiencePoints;
    private Integer difficultyClass;
    private String traits;
    private String actions;
    private String legendaryActions;
    private String reactions;
    private String lairActions;
    private String regionalEffects;
}
