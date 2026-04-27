// src/main/java/it/univ/ddmanager/world/controller/WorldController.java
package it.univ.ddmanager.world.controller;

import it.univ.ddmanager.world.dto.WorldRequest;
import it.univ.ddmanager.world.dto.WorldResponse;
import it.univ.ddmanager.world.model.World;
import it.univ.ddmanager.world.service.WorldService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worlds")
@RequiredArgsConstructor
public class WorldController {

    private final WorldService worldService;

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PostMapping
    public ResponseEntity<WorldResponse> createWorld(@Valid @RequestBody WorldRequest request) {
        World world = worldService.createWorld(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(worldService.toResponse(world));
    }

    @GetMapping
    public List<WorldResponse> getAllWorlds() {
        return worldService.getAllWorlds().stream()
                .map(worldService::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    public List<WorldResponse> getMyWorlds() {
        return worldService.getMyWorlds();
    }

    @GetMapping("/public")
    public List<WorldResponse> getPublicWorlds() {
        return worldService.getPublicWorlds();
    }

    @GetMapping("/public/{id}")
    public WorldResponse getPublicWorldById(@PathVariable Long id) {
        return worldService.getPublicWorldById(id);
    }

    @GetMapping("/{id}")
    public WorldResponse getWorldById(@PathVariable Long id) {
        return worldService.toResponse(worldService.getWorldById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PutMapping("/{id}")
    public WorldResponse updateWorld(@PathVariable Long id, @Valid @RequestBody WorldRequest request) {
        World updated = worldService.updateWorld(id, request);
        return worldService.toResponse(updated);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorld(@PathVariable Long id) {
        worldService.deleteWorld(id);
        return ResponseEntity.noContent().build();
    }
}
