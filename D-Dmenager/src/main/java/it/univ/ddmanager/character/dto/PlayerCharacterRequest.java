package it.univ.ddmanager.character.dto;

import it.univ.ddmanager.character.model.CharacterLanguage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCharacterRequest {

    private Long worldId;

    @NotBlank
    private String name;

    @NotBlank
    private String race;

    @NotBlank
    private String characterClass;

    private String subclass;

    private String background;

    private String alignment;

    @NotNull
    @Min(1)
    private Integer level;

    @Min(0)
    private Integer experiencePoints;

    private Boolean inspiration;

    private Integer proficiencyBonus;

    @Min(1)
    @Max(30)
    private Integer strength;

    @Min(1)
    @Max(30)
    private Integer dexterity;

    @Min(1)
    @Max(30)
    private Integer constitution;

    @Min(1)
    @Max(30)
    private Integer intelligence;

    @Min(1)
    @Max(30)
    private Integer wisdom;

    @Min(1)
    @Max(30)
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

    @NotNull
    @Min(1)
    private Integer maxHitPoints;

    @NotNull
    @Min(0)
    private Integer currentHitPoints;

    @NotNull
    @Min(1)
    private Integer armorClass;

    @Min(0)
    private Integer speed;

    private Integer initiativeModifier;

    @Min(0)
    private Integer temporaryHitPoints;

    private String hitDice;

    @Min(0)
    @Max(3)
    private Integer deathSaveSuccesses;

    @Min(0)
    @Max(3)
    private Integer deathSaveFailures;

    @Min(0)
    private Integer passivePerception;

    @Min(0)
    private Integer passiveInvestigation;

    @Min(0)
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
}
