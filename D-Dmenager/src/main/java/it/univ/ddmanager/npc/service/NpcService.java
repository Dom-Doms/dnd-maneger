// src/main/java/it/univ/ddmanager/npc/service/NpcService.java
package it.univ.ddmanager.npc.service;

import it.univ.ddmanager.common.ResourceNotFoundException;
import it.univ.ddmanager.npc.dto.NpcRequest;
import it.univ.ddmanager.npc.model.Npc;
import it.univ.ddmanager.npc.repository.NpcRepository;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.service.CurrentUserService;
import it.univ.ddmanager.world.model.World;
import it.univ.ddmanager.world.repository.WorldRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NpcService {

    private final NpcRepository npcRepository;
    private final WorldRepository worldRepository;
    private final CurrentUserService currentUserService;

    public List<Npc> getAllForUser(User currentUser) {
        if (isGmOrAdmin(currentUser)) {
            return npcRepository.findAll();
        }
        return npcRepository.findByIsVisibleToPlayersTrue();
    }

    public List<Npc> getByWorldForUser(Long worldId, User currentUser) {
        worldRepository.findById(Objects.requireNonNull(worldId))
                .orElseThrow(() -> new ResourceNotFoundException("World not found: " + worldId));

        if (isGmOrAdmin(currentUser)) {
            return npcRepository.findByWorldId(worldId);
        }
        return npcRepository.findByWorldIdAndIsVisibleToPlayersTrue(worldId);
    }

    public Npc getByIdForUser(Long id, User currentUser) {
        Npc npc = npcRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("NPC not found: " + id));
        if (!isGmOrAdmin(currentUser) && !npc.isVisibleToPlayers()) {
            throw new ResourceNotFoundException("NPC not found: " + id);
        }
        return npc;
    }

    public Npc create(NpcRequest request, User currentUser) {
        requireGmOrAdmin(currentUser);
        World world = worldRepository.findById(Objects.requireNonNull(request.getWorldId()))
                .orElseThrow(() -> new ResourceNotFoundException("World not found: " + request.getWorldId()));

        Npc npc = new Npc();
        npc.setWorld(world);
        npc.setOwner(currentUser);
        npc.setName(request.getName());
        applyRequestFields(npc, request);
        return npcRepository.save(Objects.requireNonNull(npc));
    }

    public Npc update(Long id, NpcRequest request, User currentUser) {
        requireGmOrAdmin(currentUser);
        Npc existing = npcRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("NPC not found: " + id));
        World world = worldRepository.findById(Objects.requireNonNull(request.getWorldId()))
                .orElseThrow(() -> new ResourceNotFoundException("World not found: " + request.getWorldId()));

        existing.setWorld(world);
        existing.setName(request.getName());
        applyRequestFields(existing, request);
        return npcRepository.save(Objects.requireNonNull(existing));
    }

    public void delete(Long id, User currentUser) {
        requireGmOrAdmin(currentUser);
        Npc npc = npcRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("NPC not found: " + id));
        npcRepository.delete(Objects.requireNonNull(npc));
    }

    private void requireGmOrAdmin(User currentUser) {
        if (!isGmOrAdmin(currentUser)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private boolean isGmOrAdmin(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(role -> "ROLE_GM".equals(role) || "ROLE_ADMIN".equals(role));
    }

    public User getCurrentUser() {
        return currentUserService.getCurrentUserOrThrow();
    }

    private void applyRequestFields(Npc npc, NpcRequest request) {
        npc.setRace(request.getRace());
        npc.setRoleOrClass(request.getRoleOrClass());
        npc.setDescription(request.getDescription());
        npc.setGmNotes(request.getGmNotes());
        npc.setVisibleToPlayers(request.getIsVisibleToPlayers() == null || request.getIsVisibleToPlayers());
        npc.setAlignment(request.getAlignment());
        npc.setSize(request.getSize());
        npc.setCreatureType(request.getCreatureType());
        npc.setArmorClass(request.getArmorClass());
        npc.setMaxHitPoints(request.getMaxHitPoints());
        npc.setCurrentHitPoints(request.getCurrentHitPoints());
        npc.setTemporaryHitPoints(request.getTemporaryHitPoints());
        npc.setHitDice(request.getHitDice());
        npc.setSpeed(request.getSpeed());
        npc.setStrength(request.getStrength());
        npc.setDexterity(request.getDexterity());
        npc.setConstitution(request.getConstitution());
        npc.setIntelligence(request.getIntelligence());
        npc.setWisdom(request.getWisdom());
        npc.setCharisma(request.getCharisma());
        npc.setSavingThrows(request.getSavingThrows());
        npc.setSkills(request.getSkills());
        npc.setDamageResistances(request.getDamageResistances());
        npc.setDamageImmunities(request.getDamageImmunities());
        npc.setConditionImmunities(request.getConditionImmunities());
        npc.setSenses(request.getSenses());
        npc.setLanguages(request.getLanguages());
        npc.setChallengeRating(request.getChallengeRating());
        npc.setExperiencePoints(request.getExperiencePoints());
        npc.setDifficultyClass(request.getDifficultyClass());
        npc.setTraits(request.getTraits());
        npc.setActions(request.getActions());
        npc.setLegendaryActions(request.getLegendaryActions());
        npc.setReactions(request.getReactions());
        npc.setLairActions(request.getLairActions());
        npc.setRegionalEffects(request.getRegionalEffects());
    }
}
