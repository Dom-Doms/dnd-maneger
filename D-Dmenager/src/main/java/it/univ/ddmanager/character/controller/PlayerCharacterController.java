package it.univ.ddmanager.character.controller;

import it.univ.ddmanager.character.dto.PlayerCharacterRequest;
import it.univ.ddmanager.character.dto.PlayerCharacterResponse;
import it.univ.ddmanager.character.dto.UpdateDeathSavesRequest;
import it.univ.ddmanager.character.dto.UpdateHitPointsRequest;
import it.univ.ddmanager.character.dto.UpdateInventoryRequest;
import it.univ.ddmanager.character.dto.UpdateSpellSlotsRequest;
import it.univ.ddmanager.character.service.PlayerCharacterService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class PlayerCharacterController {

    private final PlayerCharacterService playerCharacterService;

    @PreAuthorize("hasAnyRole('ADMIN','GM','PLAYER')")
    @PostMapping
    public ResponseEntity<PlayerCharacterResponse> create(@Valid @RequestBody PlayerCharacterRequest request) {
        PlayerCharacterResponse response = playerCharacterService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public List<PlayerCharacterResponse> getMyCharacters() {
        return playerCharacterService.getMyCharacters();
    }

    @GetMapping("/{id}")
    public PlayerCharacterResponse getById(@PathVariable Long id) {
        return playerCharacterService.getById(id);
    }

    @GetMapping("/world/{worldId}")
    public List<PlayerCharacterResponse> getByWorld(@PathVariable Long worldId) {
        return playerCharacterService.getByWorld(worldId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM','PLAYER')")
    @PutMapping("/{id}")
    public PlayerCharacterResponse update(@PathVariable Long id, @Valid @RequestBody PlayerCharacterRequest request) {
        return playerCharacterService.update(id, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM','PLAYER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        playerCharacterService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM','PLAYER')")
    @PatchMapping("/{id}/hp")
    public PlayerCharacterResponse updateHitPoints(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHitPointsRequest request) {
        return playerCharacterService.updateHitPoints(id, request.getCurrentHitPoints());
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM','PLAYER')")
    @PatchMapping("/{id}/death-saves")
    public PlayerCharacterResponse updateDeathSaves(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeathSavesRequest request) {
        return playerCharacterService.updateDeathSaves(id, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM','PLAYER')")
    @PatchMapping("/{id}/inventory")
    public PlayerCharacterResponse updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryRequest request) {
        return playerCharacterService.updateInventory(id, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM','PLAYER')")
    @PatchMapping("/{id}/spell-slots")
    public PlayerCharacterResponse updateSpellSlots(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSpellSlotsRequest request) {
        return playerCharacterService.updateSpellSlots(id, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM','PLAYER')")
    @PostMapping("/{id}/long-rest")
    public PlayerCharacterResponse performLongRest(@PathVariable Long id) {
        return playerCharacterService.performLongRest(id);
    }
}
