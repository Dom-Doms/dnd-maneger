// src/main/java/it/univ/ddmanager/item/controller/ItemController.java
package it.univ.ddmanager.item.controller;

import it.univ.ddmanager.item.dto.ItemRequest;
import it.univ.ddmanager.item.dto.ItemResponse;
import it.univ.ddmanager.item.model.Item;
import it.univ.ddmanager.item.service.ItemService;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
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
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public List<ItemResponse> getAll() {
        User currentUser = itemService.getCurrentUser();
        return itemService.getAllForUser(currentUser).stream()
            .map(item -> mapForUser(item, currentUser))
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ItemResponse getById(@PathVariable Long id) {
        User currentUser = itemService.getCurrentUser();
        Item item = itemService.getByIdForUser(id, currentUser);
        return mapForUser(item, currentUser);
    }

    @GetMapping("/world/{worldId}")
    public List<ItemResponse> getByWorld(@PathVariable Long worldId) {
        User currentUser = itemService.getCurrentUser();
        return itemService.getByWorldForUser(worldId, currentUser).stream()
            .map(item -> mapForUser(item, currentUser))
            .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemRequest request) {
        User currentUser = itemService.getCurrentUser();
        Item item = itemService.create(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponse.fromEntityForGm(item));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        User currentUser = itemService.getCurrentUser();
        Item item = itemService.update(id, request, currentUser);
        return ItemResponse.fromEntityForGm(item);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User currentUser = itemService.getCurrentUser();
        itemService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    private ItemResponse mapForUser(Item item, User user) {
        boolean gmOrAdmin = user.getRoles().stream()
            .map(Role::getName)
            .anyMatch(role -> "ROLE_GM".equals(role) || "ROLE_ADMIN".equals(role));
        return gmOrAdmin ? ItemResponse.fromEntityForGm(item) : ItemResponse.fromEntityForPlayer(item);
    }
}
