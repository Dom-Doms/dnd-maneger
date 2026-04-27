// src/main/java/it/univ/ddmanager/npc/controller/NpcController.java
package it.univ.ddmanager.npc.controller;

import it.univ.ddmanager.npc.dto.NpcRequest;
import it.univ.ddmanager.npc.dto.NpcResponse;
import it.univ.ddmanager.npc.model.Npc;
import it.univ.ddmanager.npc.service.NpcService;
import it.univ.ddmanager.user.model.User;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/npcs")
@RequiredArgsConstructor
public class NpcController {

    private final NpcService npcService;

    @GetMapping
    public List<NpcResponse> getAll() {
        User currentUser = npcService.getCurrentUser();
        return npcService.getAllForUser(currentUser).stream()
            .map(npc -> mapForUser(npc, currentUser))
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public NpcResponse getById(@PathVariable Long id) {
        User currentUser = npcService.getCurrentUser();
        Npc npc = npcService.getByIdForUser(id, currentUser);
        return mapForUser(npc, currentUser);
    }

    @GetMapping("/world/{worldId}")
    public List<NpcResponse> getByWorld(@PathVariable Long worldId) {
        User currentUser = npcService.getCurrentUser();
        return npcService.getByWorldForUser(worldId, currentUser).stream()
            .map(npc -> mapForUser(npc, currentUser))
            .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PostMapping
    public ResponseEntity<NpcResponse> create(@Valid @RequestBody NpcRequest request) {
        User currentUser = npcService.getCurrentUser();
        Npc npc = npcService.create(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(NpcResponse.fromEntity(npc, currentUser));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PutMapping("/{id}")
    public NpcResponse update(@PathVariable Long id, @Valid @RequestBody NpcRequest request) {
        User currentUser = npcService.getCurrentUser();
        Npc npc = npcService.update(id, request, currentUser);
        return NpcResponse.fromEntity(npc, currentUser);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User currentUser = npcService.getCurrentUser();
        npcService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    private NpcResponse mapForUser(Npc npc, User user) {
        return NpcResponse.fromEntity(npc, user);
    }
}
