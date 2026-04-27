package it.univ.ddmanager.character.service;

import it.univ.ddmanager.character.dto.PlayerCharacterRequest;
import it.univ.ddmanager.character.dto.PlayerCharacterResponse;
import it.univ.ddmanager.character.dto.UpdateDeathSavesRequest;
import it.univ.ddmanager.character.dto.UpdateInventoryRequest;
import it.univ.ddmanager.character.dto.UpdateSpellSlotsRequest;
import it.univ.ddmanager.character.model.CharacterLanguage;
import it.univ.ddmanager.character.model.PlayerCharacter;
import it.univ.ddmanager.character.repository.PlayerCharacterRepository;
import it.univ.ddmanager.common.ResourceNotFoundException;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.service.CurrentUserService;
import it.univ.ddmanager.world.model.World;
import it.univ.ddmanager.world.repository.WorldRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerCharacterService {

    private final PlayerCharacterRepository playerCharacterRepository;
    private final WorldRepository worldRepository;
    private final CurrentUserService currentUserService;

    public PlayerCharacterResponse create(PlayerCharacterRequest request) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requirePlayableRole(currentUser);
        PlayerCharacter character = new PlayerCharacter();
        character.setWorld(null);
        character.setOwner(currentUser);
        applyRequest(character, request);
        PlayerCharacter saved = playerCharacterRepository.save(Objects.requireNonNull(character));
        return PlayerCharacterResponse.fromEntity(saved, currentUser);
    }

    public PlayerCharacterResponse update(Long id, PlayerCharacterRequest request) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requirePlayableRole(currentUser);
        PlayerCharacter character = playerCharacterRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Player character not found: " + id));
        requireUpdatePermission(character, currentUser);
        character.setWorld(resolveWorldForUpdate(request.getWorldId(), currentUser, character.getWorld()));
        applyRequest(character, request);
        PlayerCharacter saved = playerCharacterRepository.save(Objects.requireNonNull(character));
        return PlayerCharacterResponse.fromEntity(saved, currentUser);
    }

    public void delete(Long id) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requirePlayableRole(currentUser);
        PlayerCharacter character = playerCharacterRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Player character not found: " + id));
        requireDeletePermission(character, currentUser);
        playerCharacterRepository.delete(Objects.requireNonNull(character));
    }

    public PlayerCharacterResponse updateHitPoints(Long id, Integer currentHitPoints) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requirePlayableRole(currentUser);
        PlayerCharacter character = playerCharacterRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Player character not found: " + id));
        requireUpdatePermission(character, currentUser);
        character.setCurrentHitPoints(currentHitPoints);
        PlayerCharacter saved = playerCharacterRepository.save(character);
        return PlayerCharacterResponse.fromEntity(saved, currentUser);
    }

    public PlayerCharacterResponse updateDeathSaves(Long id, UpdateDeathSavesRequest request) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requirePlayableRole(currentUser);
        PlayerCharacter character = playerCharacterRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Player character not found: " + id));
        requireUpdatePermission(character, currentUser);
        character.setDeathSaveSuccesses(clampDeathSaves(request.getSuccesses()));
        character.setDeathSaveFailures(clampDeathSaves(request.getFailures()));
        PlayerCharacter saved = playerCharacterRepository.save(character);
        return PlayerCharacterResponse.fromEntity(saved, currentUser);
    }

    public PlayerCharacterResponse updateInventory(Long id, UpdateInventoryRequest request) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requirePlayableRole(currentUser);
        PlayerCharacter character = playerCharacterRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Player character not found: " + id));
        requireUpdatePermission(character, currentUser);
        character.setEquipment(request.getEquipment());
        character.setTreasure(request.getTreasure());
        PlayerCharacter saved = playerCharacterRepository.save(character);
        return PlayerCharacterResponse.fromEntity(saved, currentUser);
    }

    public PlayerCharacterResponse updateSpellSlots(Long id, UpdateSpellSlotsRequest request) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requirePlayableRole(currentUser);
        PlayerCharacter character = playerCharacterRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Player character not found: " + id));
        requireUpdatePermission(character, currentUser);
        character.setSpellSlots(request.getSpellSlots());
        PlayerCharacter saved = playerCharacterRepository.save(character);
        return PlayerCharacterResponse.fromEntity(saved, currentUser);
    }

    public PlayerCharacterResponse performLongRest(Long id) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requirePlayableRole(currentUser);
        PlayerCharacter character = playerCharacterRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Player character not found: " + id));
        requireUpdatePermission(character, currentUser);
        if (character.getMaxHitPoints() != null) {
            character.setCurrentHitPoints(character.getMaxHitPoints());
        }
        String spellSlots = character.getSpellSlots();
        if (spellSlots != null && !spellSlots.isBlank()) {
            character.setSpellSlots(spellSlots.replaceAll("(\\d+):(\\d+)/(\\d+)", "$1:$3/$3"));
        }
        character.setDeathSaveSuccesses(0);
        character.setDeathSaveFailures(0);
        PlayerCharacter saved = playerCharacterRepository.save(character);
        return PlayerCharacterResponse.fromEntity(saved, currentUser);
    }

    public PlayerCharacterResponse getById(Long id) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        PlayerCharacter character = playerCharacterRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Player character not found: " + id));
        if (!canViewCharacter(character, currentUser)) {
            throw new ResourceNotFoundException("Player character not found: " + id);
        }
        return PlayerCharacterResponse.fromEntity(character, currentUser);
    }

    public List<PlayerCharacterResponse> getMyCharacters() {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        return playerCharacterRepository.findByOwner(currentUser).stream()
                .map(character -> PlayerCharacterResponse.fromEntity(character, currentUser))
                .collect(Collectors.toList());
    }

    public List<PlayerCharacterResponse> getByWorld(Long worldId) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        loadWorld(worldId);
        return playerCharacterRepository.findByWorldId(worldId).stream()
                .filter(character -> canViewCharacter(character, currentUser))
                .map(character -> PlayerCharacterResponse.fromEntity(character, currentUser))
                .collect(Collectors.toList());
    }

    public User getCurrentUser() {
        return currentUserService.getCurrentUserOrThrow();
    }

    private World loadWorld(Long worldId) {
        return worldRepository.findById(Objects.requireNonNull(worldId))
                .orElseThrow(() -> new ResourceNotFoundException("World not found: " + worldId));
    }

    private World resolveWorldForUpdate(Long requestedWorldId, User currentUser, World currentWorld) {
        if (requestedWorldId == null) {
            return currentWorld;
        }
        if (!isGmOrAdmin(currentUser)) {
            return currentWorld;
        }
        return loadWorld(requestedWorldId);
    }

    private void applyRequest(PlayerCharacter character, PlayerCharacterRequest request) {
        character.setName(request.getName());
        character.setRace(request.getRace());
        character.setCharacterClass(request.getCharacterClass());
        character.setSubclass(request.getSubclass());
        character.setBackground(request.getBackground());
        character.setAlignment(request.getAlignment());
        character.setLevel(request.getLevel() != null ? request.getLevel() : 1);
        character.setExperiencePoints(request.getExperiencePoints() != null ? request.getExperiencePoints() : 0);
        character.setInspiration(Boolean.TRUE.equals(request.getInspiration()));
        character.setProficiencyBonus(request.getProficiencyBonus());
        character.setStrength(request.getStrength());
        character.setDexterity(request.getDexterity());
        character.setConstitution(request.getConstitution());
        character.setIntelligence(request.getIntelligence());
        character.setWisdom(request.getWisdom());
        character.setCharisma(request.getCharisma());
        character.setStrengthSaveProficient(toBoolean(request.getStrengthSaveProficient()));
        character.setDexteritySaveProficient(toBoolean(request.getDexteritySaveProficient()));
        character.setConstitutionSaveProficient(toBoolean(request.getConstitutionSaveProficient()));
        character.setIntelligenceSaveProficient(toBoolean(request.getIntelligenceSaveProficient()));
        character.setWisdomSaveProficient(toBoolean(request.getWisdomSaveProficient()));
        character.setCharismaSaveProficient(toBoolean(request.getCharismaSaveProficient()));
        character.setAcrobaticsProficient(toBoolean(request.getAcrobaticsProficient()));
        character.setAnimalHandlingProficient(toBoolean(request.getAnimalHandlingProficient()));
        character.setArcanaProficient(toBoolean(request.getArcanaProficient()));
        character.setAthleticsProficient(toBoolean(request.getAthleticsProficient()));
        character.setDeceptionProficient(toBoolean(request.getDeceptionProficient()));
        character.setHistoryProficient(toBoolean(request.getHistoryProficient()));
        character.setInsightProficient(toBoolean(request.getInsightProficient()));
        character.setIntimidationProficient(toBoolean(request.getIntimidationProficient()));
        character.setInvestigationProficient(toBoolean(request.getInvestigationProficient()));
        character.setMedicineProficient(toBoolean(request.getMedicineProficient()));
        character.setNatureProficient(toBoolean(request.getNatureProficient()));
        character.setPerceptionProficient(toBoolean(request.getPerceptionProficient()));
        character.setPerformanceProficient(toBoolean(request.getPerformanceProficient()));
        character.setPersuasionProficient(toBoolean(request.getPersuasionProficient()));
        character.setReligionProficient(toBoolean(request.getReligionProficient()));
        character.setSleightOfHandProficient(toBoolean(request.getSleightOfHandProficient()));
        character.setStealthProficient(toBoolean(request.getStealthProficient()));
        character.setSurvivalProficient(toBoolean(request.getSurvivalProficient()));
        character.setMaxHitPoints(request.getMaxHitPoints());
        character.setCurrentHitPoints(request.getCurrentHitPoints());
        character.setArmorClass(request.getArmorClass());
        character.setSpeed(request.getSpeed());
        character.setInitiativeModifier(request.getInitiativeModifier());
        character.setTemporaryHitPoints(request.getTemporaryHitPoints());
        character.setHitDice(request.getHitDice());
        character.setDeathSaveSuccesses(clampDeathSaves(request.getDeathSaveSuccesses()));
        character.setDeathSaveFailures(clampDeathSaves(request.getDeathSaveFailures()));
        character.setPassivePerception(request.getPassivePerception());
        character.setPassiveInvestigation(request.getPassiveInvestigation());
        character.setPassiveInsight(request.getPassiveInsight());
        character.setPersonalityTraits(request.getPersonalityTraits());
        character.setIdeals(request.getIdeals());
        character.setBonds(request.getBonds());
        character.setFlaws(request.getFlaws());
        character.setAppearance(request.getAppearance());
        character.setBackstory(request.getBackstory());
        character.setFeaturesAndTraits(request.getFeaturesAndTraits());
        character.setAlliesAndOrganizations(request.getAlliesAndOrganizations());
        character.setTreasure(request.getTreasure());
        character.setOtherProficiencies(request.getOtherProficiencies());
        character.setProficienciesAndLanguages(request.getProficienciesAndLanguages());
        character.setAttacksAndSpellcasting(request.getAttacksAndSpellcasting());
        character.setEquipment(request.getEquipment());
        character.setSpells(request.getSpells());
        character.setSpellcastingClass(request.getSpellcastingClass());
        character.setSpellSaveDC(request.getSpellSaveDC());
        character.setSpellAttackBonus(request.getSpellAttackBonus());
        character.setKnownSpells(request.getKnownSpells());
        character.setPreparedSpells(request.getPreparedSpells());
        character.setSpellSlots(request.getSpellSlots());
        character.setOtherNotes(request.getOtherNotes());
        character.setGmNotes(request.getGmNotes());
        character.setVisibleToPlayers(request.getIsVisibleToPlayers() == null || request.getIsVisibleToPlayers());
        if (request.getKnownLanguages() != null) {
            character.setKnownLanguages(sanitizeLanguages(request.getKnownLanguages()));
        } else if (character.getKnownLanguages() == null || character.getKnownLanguages().isEmpty()) {
            character.setKnownLanguages(EnumSet.of(CharacterLanguage.COMMON));
        } else if (!character.getKnownLanguages().contains(CharacterLanguage.COMMON)) {
            Set<CharacterLanguage> existing = EnumSet.copyOf(character.getKnownLanguages());
            existing.add(CharacterLanguage.COMMON);
            character.setKnownLanguages(existing);
        }
    }

    private void requireUpdatePermission(PlayerCharacter character, User currentUser) {
        if (isOwner(character, currentUser)) {
            return;
        }
        if (isGmOrAdmin(currentUser)) {
            return;
        }
        throw new AccessDeniedException("Access is denied");
    }

    private void requireDeletePermission(PlayerCharacter character, User currentUser) {
        if (isOwner(character, currentUser)) {
            return;
        }
        if (isAdmin(currentUser)) {
            return;
        }
        throw new AccessDeniedException("Access is denied");
    }

    private boolean canViewCharacter(PlayerCharacter character, User user) {
        if (user == null) {
            return character.isVisibleToPlayers();
        }
        if (isOwner(character, user) || isGmOrAdmin(user)) {
            return true;
        }
        return character.isVisibleToPlayers();
    }

    private boolean isOwner(PlayerCharacter character, User user) {
        return character.getOwner() != null
                && character.getOwner().getId() != null
                && character.getOwner().getId().equals(user.getId());
    }

    private void requirePlayableRole(User user) {
        if (!hasPlayableRole(user)) {
            throw new AccessDeniedException("Only players or game masters can manage characters");
        }
    }

    private boolean hasPlayableRole(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(role -> "ROLE_PLAYER".equals(role)
                        || "ROLE_GM".equals(role)
                        || "ROLE_ADMIN".equals(role));
    }

    private boolean isGmOrAdmin(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(role -> "ROLE_GM".equals(role) || "ROLE_ADMIN".equals(role));
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private boolean toBoolean(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private int clampDeathSaves(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(3, value));
    }

    private Set<CharacterLanguage> sanitizeLanguages(Set<CharacterLanguage> languages) {
        if (languages == null || languages.isEmpty()) {
            return EnumSet.of(CharacterLanguage.COMMON);
        }
        EnumSet<CharacterLanguage> sanitized = EnumSet.copyOf(languages);
        sanitized.add(CharacterLanguage.COMMON);
        return sanitized;
    }
}
