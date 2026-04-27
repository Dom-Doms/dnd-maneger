// src/main/java/it/univ/ddmanager/npc/dto/NpcResponse.java
package it.univ.ddmanager.npc.dto;

import it.univ.ddmanager.npc.model.Npc;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NpcResponse {

    private Long id;
    private Long worldId;
    private Long ownerId;
    private String ownerNickname;
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

    public static NpcResponse fromEntity(Npc npc, User viewer) {
        boolean canViewGmNotes = canViewPrivateDetails(npc, viewer);
        NpcResponse response = new NpcResponse();
        response.setId(npc.getId());
        response.setWorldId(npc.getWorld() != null ? npc.getWorld().getId() : null);
        response.setOwnerId(npc.getOwner() != null ? npc.getOwner().getId() : null);
        response.setOwnerNickname(npc.getOwner() != null ? npc.getOwner().getNickname() : null);
        response.setName(npc.getName());
        response.setRace(npc.getRace());
        response.setRoleOrClass(npc.getRoleOrClass());
        response.setDescription(npc.getDescription());
        response.setGmNotes(canViewGmNotes ? npc.getGmNotes() : null);
        response.setIsVisibleToPlayers(npc.isVisibleToPlayers());
        response.setAlignment(npc.getAlignment());
        response.setSize(npc.getSize());
        response.setCreatureType(npc.getCreatureType());
        response.setArmorClass(npc.getArmorClass());
        response.setMaxHitPoints(npc.getMaxHitPoints());
        response.setCurrentHitPoints(npc.getCurrentHitPoints());
        response.setTemporaryHitPoints(npc.getTemporaryHitPoints());
        response.setHitDice(npc.getHitDice());
        response.setSpeed(npc.getSpeed());
        response.setStrength(npc.getStrength());
        response.setDexterity(npc.getDexterity());
        response.setConstitution(npc.getConstitution());
        response.setIntelligence(npc.getIntelligence());
        response.setWisdom(npc.getWisdom());
        response.setCharisma(npc.getCharisma());
        response.setSavingThrows(npc.getSavingThrows());
        response.setSkills(npc.getSkills());
        response.setDamageResistances(npc.getDamageResistances());
        response.setDamageImmunities(npc.getDamageImmunities());
        response.setConditionImmunities(npc.getConditionImmunities());
        response.setSenses(npc.getSenses());
        response.setLanguages(npc.getLanguages());
        response.setChallengeRating(npc.getChallengeRating());
        response.setExperiencePoints(npc.getExperiencePoints());
        response.setDifficultyClass(npc.getDifficultyClass());
        response.setTraits(npc.getTraits());
        response.setActions(npc.getActions());
        response.setLegendaryActions(npc.getLegendaryActions());
        response.setReactions(npc.getReactions());
        response.setLairActions(npc.getLairActions());
        response.setRegionalEffects(npc.getRegionalEffects());
        return response;
    }

    private static boolean canViewPrivateDetails(Npc npc, User viewer) {
        if (viewer == null) {
            return false;
        }
        if (npc.getOwner() != null && npc.getOwner().getId() != null
            && npc.getOwner().getId().equals(viewer.getId())) {
            return true;
        }
        return viewer.getRoles().stream()
            .map(Role::getName)
            .anyMatch(role -> "ROLE_GM".equals(role) || "ROLE_ADMIN".equals(role));
    }
}
