// src/main/java/it/univ/ddmanager/npc/model/Npc.java
package it.univ.ddmanager.npc.model;

import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.world.model.World;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "npcs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Npc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "world_id", nullable = false)
    private World world;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    private String race;

    private String roleOrClass;

    private String description;

    private String gmNotes;

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

    @Column(nullable = false)
    private boolean isVisibleToPlayers = true;
}
