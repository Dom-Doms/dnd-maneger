package it.univ.ddmanager.character.model;

import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.world.model.World;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player_characters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "world_id", nullable = true)
    private World world;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    private String race;

    @Column(name = "character_class")
    private String characterClass;

    private String subclass;

    private String background;

    private String alignment;

    @Column(nullable = false)
    private Integer level = 1;

    @Column(nullable = false)
    private Integer experiencePoints = 0;

    @Column(nullable = false)
    private boolean inspiration = false;

    private Integer proficiencyBonus;

    private Integer strength;
    private Integer dexterity;
    private Integer constitution;
    private Integer intelligence;
    private Integer wisdom;
    private Integer charisma;

    @Column(nullable = false)
    private boolean strengthSaveProficient = false;

    @Column(nullable = false)
    private boolean dexteritySaveProficient = false;

    @Column(nullable = false)
    private boolean constitutionSaveProficient = false;

    @Column(nullable = false)
    private boolean intelligenceSaveProficient = false;

    @Column(nullable = false)
    private boolean wisdomSaveProficient = false;

    @Column(nullable = false)
    private boolean charismaSaveProficient = false;

    @Column(nullable = false)
    private boolean acrobaticsProficient = false;

    @Column(nullable = false)
    private boolean animalHandlingProficient = false;

    @Column(nullable = false)
    private boolean arcanaProficient = false;

    @Column(nullable = false)
    private boolean athleticsProficient = false;

    @Column(nullable = false)
    private boolean deceptionProficient = false;

    @Column(nullable = false)
    private boolean historyProficient = false;

    @Column(nullable = false)
    private boolean insightProficient = false;

    @Column(nullable = false)
    private boolean intimidationProficient = false;

    @Column(nullable = false)
    private boolean investigationProficient = false;

    @Column(nullable = false)
    private boolean medicineProficient = false;

    @Column(nullable = false)
    private boolean natureProficient = false;

    @Column(nullable = false)
    private boolean perceptionProficient = false;

    @Column(nullable = false)
    private boolean performanceProficient = false;

    @Column(nullable = false)
    private boolean persuasionProficient = false;

    @Column(nullable = false)
    private boolean religionProficient = false;

    @Column(nullable = false)
    private boolean sleightOfHandProficient = false;

    @Column(nullable = false)
    private boolean stealthProficient = false;

    @Column(nullable = false)
    private boolean survivalProficient = false;

    private Integer maxHitPoints;
    private Integer currentHitPoints;
    private Integer armorClass;
    private Integer speed;
    private Integer initiativeModifier;
    private Integer temporaryHitPoints;
    private String hitDice;

    @Column(nullable = false)
    private Integer deathSaveSuccesses = 0;

    @Column(nullable = false)
    private Integer deathSaveFailures = 0;

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

    @Column(nullable = false)
    private boolean isVisibleToPlayers = true;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = CharacterLanguage.class)
    @CollectionTable(name = "player_character_languages", joinColumns = @JoinColumn(name = "character_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private Set<CharacterLanguage> knownLanguages = new HashSet<>();
}
