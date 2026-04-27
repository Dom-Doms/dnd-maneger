// src/main/java/it/univ/ddmanager/campaign/service/CampaignService.java
package it.univ.ddmanager.campaign.service;

import it.univ.ddmanager.campaign.dto.CampaignRequest;
import it.univ.ddmanager.campaign.model.Campaign;
import it.univ.ddmanager.campaign.model.CampaignStatus;
import it.univ.ddmanager.campaign.repository.CampaignRepository;
import it.univ.ddmanager.common.ResourceNotFoundException;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.service.CurrentUserService;
import it.univ.ddmanager.world.model.World;
import it.univ.ddmanager.world.repository.WorldRepository;
import it.univ.ddmanager.world.service.WorldService;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final WorldRepository worldRepository;
    private final WorldService worldService;
    private final CurrentUserService currentUserService;

    public Campaign createCampaign(CampaignRequest request) {
        World world = worldRepository.findById(Objects.requireNonNull(request.getWorldId()))
                .orElseThrow(() -> new ResourceNotFoundException("World not found: " + request.getWorldId()));

        User owner = currentUserService.getCurrentUserOrThrow();

        if (campaignRepository.existsByWorldIdAndNameIgnoreCase(world.getId(), request.getName())) {
            throw new IllegalArgumentException("Campaign name already exists in this world: " + request.getName());
        }

        CampaignStatus status = resolveStatus(request.getStatus());

        Campaign campaign = new Campaign();
        campaign.setName(request.getName());
        campaign.setDescription(request.getDescription());
        campaign.setStatus(status);
        campaign.setWorld(world);
        campaign.setOwner(owner);

        return campaignRepository.save(Objects.requireNonNull(campaign));
    }

    public List<Campaign> getAllCampaigns() {
        List<Long> accessibleWorldIds = worldService.getAllWorlds().stream()
                .map(World::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (accessibleWorldIds.isEmpty()) {
            return List.of();
        }
        return campaignRepository.findByWorldIdIn(accessibleWorldIds);
    }

    public List<Campaign> getMyCampaigns() {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        return campaignRepository.findByOwner(currentUser);
    }

    public Campaign getCampaignById(Long id) {
        Campaign campaign = campaignRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + id));
        if (campaign.getWorld() != null && campaign.getWorld().getId() != null) {
            worldService.getWorldById(campaign.getWorld().getId());
        } else {
            currentUserService.getCurrentUserOrThrow();
        }
        return campaign;
    }

    public List<Campaign> getCampaignsByWorld(Long worldId) {
        worldService.getWorldById(worldId);
        return campaignRepository.findByWorldId(worldId);
    }

    public Campaign updateCampaign(Long id, CampaignRequest request) {
        Campaign campaign = campaignRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id " + id));
        User currentUser = currentUserService.getCurrentUserOrThrow();
        enforceOwnership(campaign, currentUser);

        World targetWorld = campaign.getWorld();
        if (request.getWorldId() != null && !request.getWorldId().equals(campaign.getWorld().getId())) {
            targetWorld = worldRepository.findById(Objects.requireNonNull(request.getWorldId()))
                    .orElseThrow(() -> new ResourceNotFoundException("World not found: " + request.getWorldId()));
        }

        if (campaignRepository.existsByWorldIdAndNameIgnoreCaseAndIdNot(targetWorld.getId(), request.getName(), id)) {
            throw new IllegalArgumentException("Campaign name already exists in this world: " + request.getName());
        }

        campaign.setWorld(targetWorld);
        campaign.setName(request.getName());
        campaign.setDescription(request.getDescription());
        campaign.setStatus(resolveStatus(request.getStatus()));

        return campaignRepository.save(Objects.requireNonNull(campaign));
    }

    public void deleteCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id " + id));
        User currentUser = currentUserService.getCurrentUserOrThrow();
        enforceOwnership(campaign, currentUser);
        campaignRepository.delete(Objects.requireNonNull(campaign));
    }

    private CampaignStatus resolveStatus(String requestedStatus) {
        if (requestedStatus == null || requestedStatus.isBlank()) {
            return CampaignStatus.ACTIVE;
        }
        try {
            return CampaignStatus.valueOf(requestedStatus.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid campaign status: " + requestedStatus);
        }
    }

    private void enforceOwnership(Campaign campaign, User currentUser) {
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        boolean isOwner = campaign.getOwner() != null && campaign.getOwner().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Access is denied");
        }
    }
}
