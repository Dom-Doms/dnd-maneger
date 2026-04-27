// src/main/java/it/univ/ddmanager/location/controller/LocationController.java
package it.univ.ddmanager.location.controller;

import it.univ.ddmanager.location.dto.LocationRequest;
import it.univ.ddmanager.location.dto.LocationResponse;
import it.univ.ddmanager.location.model.Location;
import it.univ.ddmanager.location.service.LocationService;
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
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public List<LocationResponse> getAll() {
        User currentUser = locationService.getCurrentUser();
        return locationService.getAllForUser(currentUser).stream()
            .map(location -> mapForUser(location, currentUser))
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public LocationResponse getById(@PathVariable Long id) {
        User currentUser = locationService.getCurrentUser();
        Location location = locationService.getByIdForUser(id, currentUser);
        return mapForUser(location, currentUser);
    }

    @GetMapping("/world/{worldId}")
    public List<LocationResponse> getByWorld(@PathVariable Long worldId) {
        User currentUser = locationService.getCurrentUser();
        return locationService.getByWorldForUser(worldId, currentUser).stream()
            .map(location -> mapForUser(location, currentUser))
            .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PostMapping
    public ResponseEntity<LocationResponse> create(@Valid @RequestBody LocationRequest request) {
        User currentUser = locationService.getCurrentUser();
        Location location = locationService.create(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(LocationResponse.fromEntityForGm(location));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PutMapping("/{id}")
    public LocationResponse update(@PathVariable Long id, @Valid @RequestBody LocationRequest request) {
        User currentUser = locationService.getCurrentUser();
        Location location = locationService.update(id, request, currentUser);
        return LocationResponse.fromEntityForGm(location);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User currentUser = locationService.getCurrentUser();
        locationService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    private LocationResponse mapForUser(Location location, User user) {
        boolean gmOrAdmin = user.getRoles().stream()
            .map(Role::getName)
            .anyMatch(role -> "ROLE_GM".equals(role) || "ROLE_ADMIN".equals(role));
        return gmOrAdmin ? LocationResponse.fromEntityForGm(location) : LocationResponse.fromEntityForPlayer(location);
    }
}
