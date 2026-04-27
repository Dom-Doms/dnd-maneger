// src/main/java/it/univ/ddmanager/world/service/WorldService.java
package it.univ.ddmanager.world.service;

import it.univ.ddmanager.campaign.repository.CampaignRepository;
import it.univ.ddmanager.common.ResourceNotFoundException;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.service.CurrentUserService;
import it.univ.ddmanager.world.dto.WorldRequest;
import it.univ.ddmanager.world.dto.WorldResponse;
import it.univ.ddmanager.world.model.World;
import it.univ.ddmanager.world.repository.WorldRepository;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorldService {

    private final WorldRepository worldRepository;
    private final CampaignRepository campaignRepository;
    private final CurrentUserService currentUserService;

    public World createWorld(WorldRequest request) {
        User owner = currentUserService.getCurrentUserOrThrow();
        World world = new World();
        world.setName(request.getName());
        world.setDescription(request.getDescription());
        world.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        world.setOwner(owner);
        return worldRepository.save(Objects.requireNonNull(world));
    }

    public List<World> getAllWorlds() {
        // Enforce ownership: users should only see their own worlds in general lists
        // (e.g. dropdowns)
        User currentUser = currentUserService.getCurrentUserOrThrow();
        return worldRepository.findByOwner(currentUser);
    }

    public List<WorldResponse> getPublicWorlds() {
        return worldRepository.findByIsPublicTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<WorldResponse> getMyWorlds() {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        return worldRepository.findByOwner(currentUser).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public World getWorldById(Long id) {
        return worldRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("World not found: " + id));
    }

    public WorldResponse getPublicWorldById(Long id) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        World world = getWorldById(id);
        if (Boolean.TRUE.equals(world.getIsPublic()) || canViewPrivateWorld(world, currentUser)) {
            return toResponse(world);
        }
        throw new ResourceNotFoundException("World not found: " + id);
    }

    public WorldResponse toResponse(World world) {
        Long campaignCount = world.getId() != null ? campaignRepository.countByWorldId(world.getId()) : 0L;
        return WorldResponse.fromEntity(world, campaignCount);
    }

    public World updateWorld(Long id, WorldRequest request) {
        World world = worldRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("World not found with id " + id));
        User currentUser = currentUserService.getCurrentUserOrThrow();
        enforceOwnership(world, currentUser);
        world.setName(request.getName());
        world.setDescription(request.getDescription());
        if (request.getIsPublic() != null) {
            world.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        }
        return worldRepository.save(Objects.requireNonNull(world));
    }

    public void deleteWorld(Long id) {
        World world = worldRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("World not found with id " + id));
        User currentUser = currentUserService.getCurrentUserOrThrow();
        enforceOwnership(world, currentUser);
        worldRepository.delete(Objects.requireNonNull(world));
    }

    private void enforceOwnership(World world, User currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access is denied");
        }
        boolean isAdmin = hasRole(currentUser, "ROLE_ADMIN");
        boolean isOwner = world.getOwner() != null && world.getOwner().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private boolean canViewPrivateWorld(World world, User user) {
        if (user == null) {
            return false;
        }
        boolean isOwner = world.getOwner() != null
                && world.getOwner().getId() != null
                && world.getOwner().getId().equals(user.getId());
        return isOwner || hasRole(user, "ROLE_ADMIN") || hasRole(user, "ROLE_GM");
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(roleName::equals);
    }
}
