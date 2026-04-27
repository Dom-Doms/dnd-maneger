package it.univ.ddmanager.character.dto;

import it.univ.ddmanager.character.model.CharacterLanguage;
import it.univ.ddmanager.character.model.PlayerCharacter;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlayerCharacterResponse {

    private Long id;
    private Long worldId;
    private Long ownerId;
    private String ownerNickname;
    private String name;
    private String race;
    private String characterClass;
    private String subclass;
    private String background;
    private String alignment;
    private Integer level;
    private Integer experiencePoints;
    private Boolean inspiration;
    private Integer proficiencyBonus;
    private Integer strength;
    private Integer dexterity;
    private Integer constitution;
    private Integer intelligence;
    private Integer wisdom;
    private Integer charisma;
    private Boolean strengthSaveProficient;
    private Boolean dexteritySaveProficient;
    private Boolean constitutionSaveProficient;
    private Boolean intelligenceSaveProficient;
    private Boolean wisdomSaveProficient;
    private Boolean charismaSaveProficient;
    private Boolean acrobaticsProficient;
    private Boolean animalHandlingProficient;
    private Boolean arcanaProficient;
    private Boolean athleticsProficient;
    private Boolean deceptionProficient;
    private Boolean historyProficient;
    private Boolean insightProficient;
    private Boolean intimidationProficient;
    private Boolean investigationProficient;
    private Boolean medicineProficient;
    private Boolean natureProficient;
    private Boolean perceptionProficient;
    private Boolean performanceProficient;
    private Boolean persuasionProficient;
    private Boolean religionProficient;
    private Boolean sleightOfHandProficient;
    private Boolean stealthProficient;
    private Boolean survivalProficient;
    private Integer maxHitPoints;
    private Integer currentHitPoints;
    private Integer armorClass;
    private Integer speed;
    private Integer initiativeModifier;
    private Integer temporaryHitPoints;
    private String hitDice;
    private Integer deathSaveSuccesses;
    private Integer deathSaveFailures;
    private Integer passivePerception;
    private Integer passiveInvestigation;
    private Integer passiveInsight;
    private String personalityTraits;
    private String ideals;
    private String bonds;
    private String flaws;
    private String appearance;
    private String backstory;
    private String featuresAndTraits;
    private String alliesAndOrganizations;
    private String treasure;
    private String otherProficiencies;
    private String proficienciesAndLanguages;
    private String attacksAndSpellcasting;
    private String equipment;
    private String spells;
    private String spellcastingClass;
    private Integer spellSaveDC;
    private Integer spellAttackBonus;
    private String knownSpells;
    private String preparedSpells;
    private String spellSlots;
    private String otherNotes;
    private String gmNotes;
    private Boolean isVisibleToPlayers;
    private Set<CharacterLanguage> knownLanguages;

    public static PlayerCharacterResponse fromEntity(PlayerCharacter character, User viewer) {
        boolean canSeeGmNotes = canViewPrivateDetails(character, viewer);
        PlayerCharacterResponse response = new PlayerCharacterResponse();
        response.setId(character.getId());
        response.setWorldId(character.getWorld() != null ? character.getWorld().getId() : null);
        response.setOwnerId(character.getOwner() != null ? character.getOwner().getId() : null);
        response.setOwnerNickname(character.getOwner() != null ? character.getOwner().getNickname() : null);
        response.setName(character.getName());
        response.setRace(character.getRace());
        response.setCharacterClass(character.getCharacterClass());
        response.setSubclass(character.getSubclass());
        response.setBackground(character.getBackground());
        response.setAlignment(character.getAlignment());
        response.setLevel(character.getLevel());
        response.setExperiencePoints(character.getExperiencePoints());
        response.setInspiration(character.isInspiration());
        response.setProficiencyBonus(character.getProficiencyBonus());
        response.setStrength(character.getStrength());
        response.setDexterity(character.getDexterity());
        response.setConstitution(character.getConstitution());
        response.setIntelligence(character.getIntelligence());
        response.setWisdom(character.getWisdom());
        response.setCharisma(character.getCharisma());
        response.setStrengthSaveProficient(character.isStrengthSaveProficient());
        response.setDexteritySaveProficient(character.isDexteritySaveProficient());
        response.setConstitutionSaveProficient(character.isConstitutionSaveProficient());
        response.setIntelligenceSaveProficient(character.isIntelligenceSaveProficient());
        response.setWisdomSaveProficient(character.isWisdomSaveProficient());
        response.setCharismaSaveProficient(character.isCharismaSaveProficient());
        response.setAcrobaticsProficient(character.isAcrobaticsProficient());
        response.setAnimalHandlingProficient(character.isAnimalHandlingProficient());
        response.setArcanaProficient(character.isArcanaProficient());
        response.setAthleticsProficient(character.isAthleticsProficient());
        response.setDeceptionProficient(character.isDeceptionProficient());
        response.setHistoryProficient(character.isHistoryProficient());
        response.setInsightProficient(character.isInsightProficient());
        response.setIntimidationProficient(character.isIntimidationProficient());
        response.setInvestigationProficient(character.isInvestigationProficient());
        response.setMedicineProficient(character.isMedicineProficient());
        response.setNatureProficient(character.isNatureProficient());
        response.setPerceptionProficient(character.isPerceptionProficient());
        response.setPerformanceProficient(character.isPerformanceProficient());
        response.setPersuasionProficient(character.isPersuasionProficient());
        response.setReligionProficient(character.isReligionProficient());
        response.setSleightOfHandProficient(character.isSleightOfHandProficient());
        response.setStealthProficient(character.isStealthProficient());
        response.setSurvivalProficient(character.isSurvivalProficient());
        response.setMaxHitPoints(character.getMaxHitPoints());
        response.setCurrentHitPoints(character.getCurrentHitPoints());
        response.setArmorClass(character.getArmorClass());
        response.setSpeed(character.getSpeed());
        response.setInitiativeModifier(character.getInitiativeModifier());
        response.setTemporaryHitPoints(character.getTemporaryHitPoints());
        response.setHitDice(character.getHitDice());
        response.setDeathSaveSuccesses(character.getDeathSaveSuccesses());
        response.setDeathSaveFailures(character.getDeathSaveFailures());
        response.setPassivePerception(character.getPassivePerception());
        response.setPassiveInvestigation(character.getPassiveInvestigation());
        response.setPassiveInsight(character.getPassiveInsight());
        response.setPersonalityTraits(character.getPersonalityTraits());
        response.setIdeals(character.getIdeals());
        response.setBonds(character.getBonds());
        response.setFlaws(character.getFlaws());
        response.setAppearance(character.getAppearance());
        response.setBackstory(character.getBackstory());
        response.setFeaturesAndTraits(character.getFeaturesAndTraits());
        response.setAlliesAndOrganizations(character.getAlliesAndOrganizations());
        response.setTreasure(character.getTreasure());
        response.setOtherProficiencies(character.getOtherProficiencies());
        response.setProficienciesAndLanguages(character.getProficienciesAndLanguages());
        response.setAttacksAndSpellcasting(character.getAttacksAndSpellcasting());
        response.setEquipment(character.getEquipment());
        response.setSpells(character.getSpells());
        response.setSpellcastingClass(character.getSpellcastingClass());
        response.setSpellSaveDC(character.getSpellSaveDC());
        response.setSpellAttackBonus(character.getSpellAttackBonus());
        response.setKnownSpells(character.getKnownSpells());
        response.setPreparedSpells(character.getPreparedSpells());
        response.setSpellSlots(character.getSpellSlots());
        response.setOtherNotes(character.getOtherNotes());
        response.setGmNotes(canSeeGmNotes ? character.getGmNotes() : null);
        response.setIsVisibleToPlayers(character.isVisibleToPlayers());
        response.setKnownLanguages(readOnlyLanguages(character));
        return response;
    }

    private static boolean canViewPrivateDetails(PlayerCharacter character, User viewer) {
        if (viewer == null) {
            return false;
        }
        if (character.getOwner() != null && character.getOwner().getId() != null
            && character.getOwner().getId().equals(viewer.getId())) {
            return true;
        }
        return viewer.getRoles().stream()
            .map(Role::getName)
            .anyMatch(role -> "ROLE_GM".equals(role) || "ROLE_ADMIN".equals(role));
    }

    private static Set<CharacterLanguage> readOnlyLanguages(PlayerCharacter character) {
        if (character.getKnownLanguages() == null || character.getKnownLanguages().isEmpty()) {
            return Collections.unmodifiableSet(EnumSet.of(CharacterLanguage.COMMON));
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(character.getKnownLanguages()));
    }
}
