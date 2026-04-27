// src/main/java/it/univ/ddmanager/item/service/ItemService.java
package it.univ.ddmanager.item.service;

import it.univ.ddmanager.common.ResourceNotFoundException;
import it.univ.ddmanager.item.dto.ItemRequest;
import it.univ.ddmanager.item.model.Item;
import it.univ.ddmanager.item.repository.ItemRepository;
import it.univ.ddmanager.location.model.Location;
import it.univ.ddmanager.location.repository.LocationRepository;
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
public class ItemService {

    private final ItemRepository itemRepository;
    private final WorldRepository worldRepository;
    private final LocationRepository locationRepository;
    private final CurrentUserService currentUserService;

    public List<Item> getAllForUser(User currentUser) {
        if (isGmOrAdmin(currentUser)) {
            return itemRepository.findAll();
        }
        return itemRepository.findByIsVisibleToPlayersTrue();
    }

    public List<Item> getByWorldForUser(Long worldId, User currentUser) {
        worldRepository.findById(Objects.requireNonNull(worldId))
                .orElseThrow(() -> new ResourceNotFoundException("World not found with id " + worldId));
        if (isGmOrAdmin(currentUser)) {
            return itemRepository.findByWorldId(worldId);
        }
        return itemRepository.findByWorldIdAndIsVisibleToPlayersTrue(worldId);
    }

    public Item getByIdForUser(Long id, User currentUser) {
        Item item = itemRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));
        if (!isGmOrAdmin(currentUser) && !item.isVisibleToPlayers()) {
            throw new ResourceNotFoundException("Item not found with id " + id);
        }
        return item;
    }

    public Item create(ItemRequest request, User currentUser) {
        requireGmOrAdmin(currentUser);
        World world = worldRepository.findById(Objects.requireNonNull(request.getWorldId()))
                .orElseThrow(() -> new ResourceNotFoundException("World not found with id " + request.getWorldId()));
        Location location = resolveLocation(request.getLocationId());

        Item item = new Item();
        item.setWorld(world);
        item.setLocation(location);
        item.setOwner(currentUser);
        item.setName(request.getName());
        item.setType(request.getType());
        item.setRarity(request.getRarity());
        item.setDescription(request.getDescription());
        item.setGmNotes(request.getGmNotes());
        item.setVisibleToPlayers(request.getIsVisibleToPlayers() == null || request.getIsVisibleToPlayers());

        return itemRepository.save(Objects.requireNonNull(item));
    }

    public Item update(Long id, ItemRequest request, User currentUser) {
        requireGmOrAdmin(currentUser);
        Item existing = itemRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));
        World world = worldRepository.findById(Objects.requireNonNull(request.getWorldId()))
                .orElseThrow(() -> new ResourceNotFoundException("World not found with id " + request.getWorldId()));
        Location location = resolveLocation(request.getLocationId());

        existing.setWorld(world);
        existing.setLocation(location);
        existing.setName(request.getName());
        existing.setType(request.getType());
        existing.setRarity(request.getRarity());
        existing.setDescription(request.getDescription());
        existing.setGmNotes(request.getGmNotes());
        existing.setVisibleToPlayers(request.getIsVisibleToPlayers() == null || request.getIsVisibleToPlayers());

        return itemRepository.save(Objects.requireNonNull(existing));
    }

    public void delete(Long id, User currentUser) {
        requireGmOrAdmin(currentUser);
        Item item = itemRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));
        itemRepository.delete(Objects.requireNonNull(item));
    }

    public User getCurrentUser() {
        return currentUserService.getCurrentUserOrThrow();
    }

    private Location resolveLocation(Long locationId) {
        if (locationId == null) {
            return null;
        }
        return locationRepository.findById(Objects.requireNonNull(locationId))
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id " + locationId));
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
}
