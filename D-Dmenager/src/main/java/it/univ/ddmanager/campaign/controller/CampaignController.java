// src/main/java/it/univ/ddmanager/campaign/controller/CampaignController.java
package it.univ.ddmanager.campaign.controller;

import it.univ.ddmanager.campaign.dto.CampaignRequest;
import it.univ.ddmanager.campaign.dto.CampaignResponse;
import it.univ.ddmanager.campaign.model.Campaign;
import it.univ.ddmanager.campaign.service.CampaignService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CampaignRequest request) {
        Campaign campaign = campaignService.createCampaign(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CampaignResponse.fromEntity(campaign));
    }

    @GetMapping
    public List<CampaignResponse> getAllCampaigns() {
        return campaignService.getAllCampaigns().stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public CampaignResponse getCampaignById(@PathVariable Long id) {
        return CampaignResponse.fromEntity(campaignService.getCampaignById(id));
    }

    @GetMapping("/world/{worldId}")
    public List<CampaignResponse> getCampaignsByWorld(@PathVariable Long worldId) {
        return campaignService.getCampaignsByWorld(worldId).stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @GetMapping("/my")
    public List<CampaignResponse> getMyCampaigns() {
        return campaignService.getMyCampaigns().stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PutMapping("/{id}")
    public CampaignResponse updateCampaign(@PathVariable Long id, @Valid @RequestBody CampaignRequest request) {
        return CampaignResponse.fromEntity(campaignService.updateCampaign(id, request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }
}
