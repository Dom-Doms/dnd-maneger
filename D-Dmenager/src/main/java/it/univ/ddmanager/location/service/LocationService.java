// src/main/java/it/univ/ddmanager/location/service/LocationService.java
package it.univ.ddmanager.location.service;

import it.univ.ddmanager.common.ResourceNotFoundException;
import it.univ.ddmanager.location.dto.LocationRequest;
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
public class LocationService {

    private final LocationRepository locationRepository;
    private final WorldRepository worldRepository;
    private final CurrentUserService currentUserService;

    public List<Location> getAllForUser(User currentUser) {
        if (isGmOrAdmin(currentUser)) {
            return locationRepository.findAll();
        }
        return locationRepository.findByIsVisibleToPlayersTrue();
    }

    public List<Location> getByWorldForUser(Long worldId, User currentUser) {
        worldRepository.findById(Objects.requireNonNull(worldId))
                .orElseThrow(() -> new ResourceNotFoundException("World not found with id " + worldId));
        if (isGmOrAdmin(currentUser)) {
            return locationRepository.findByWorldId(worldId);
        }
        return locationRepository.findByWorldIdAndIsVisibleToPlayersTrue(worldId);
    }

    public Location getByIdForUser(Long id, User currentUser) {
        Location location = locationRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id " + id));
        if (!isGmOrAdmin(currentUser) && !location.isVisibleToPlayers()) {
            throw new ResourceNotFoundException("Location not found with id " + id);
        }
        return location;
    }

    public Location create(LocationRequest request, User currentUser) {
        requireGmOrAdmin(currentUser);
        World world = worldRepository.findById(Objects.requireNonNull(request.getWorldId()))
                .orElseThrow(() -> new ResourceNotFoundException("World not found with id " + request.getWorldId()));
        Location parent = resolveParent(request.getParentLocationId());

        Location location = new Location();
        location.setWorld(world);
        location.setParentLocation(parent);
        location.setOwner(currentUser);
        location.setName(request.getName());
        location.setType(request.getType());
        location.setDescription(request.getDescription());
        location.setGmNotes(request.getGmNotes());
        location.setVisibleToPlayers(request.getIsVisibleToPlayers() == null || request.getIsVisibleToPlayers());

        return locationRepository.save(Objects.requireNonNull(location));
    }

    public Location update(Long id, LocationRequest request, User currentUser) {
        requireGmOrAdmin(currentUser);
        Location existing = locationRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id " + id));
        World world = worldRepository.findById(Objects.requireNonNull(request.getWorldId()))
                .orElseThrow(() -> new ResourceNotFoundException("World not found with id " + request.getWorldId()));
        Location parent = resolveParent(request.getParentLocationId());

        existing.setWorld(world);
        existing.setParentLocation(parent);
        existing.setName(request.getName());
        existing.setType(request.getType());
        existing.setDescription(request.getDescription());
        existing.setGmNotes(request.getGmNotes());
        existing.setVisibleToPlayers(request.getIsVisibleToPlayers() == null || request.getIsVisibleToPlayers());

        return locationRepository.save(Objects.requireNonNull(existing));
    }

    public void delete(Long id, User currentUser) {
        requireGmOrAdmin(currentUser);
        Location location = locationRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id " + id));
        locationRepository.delete(Objects.requireNonNull(location));
    }

    public User getCurrentUser() {
        return currentUserService.getCurrentUserOrThrow();
    }

    private Location resolveParent(Long parentLocationId) {
        if (parentLocationId == null) {
            return null;
        }
        return locationRepository.findById(Objects.requireNonNull(parentLocationId))
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id " + parentLocationId));
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
