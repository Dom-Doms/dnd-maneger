package it.univ.ddmanager.campaign.controller;

import it.univ.ddmanager.campaign.dto.CampaignPlayerRequest;
import it.univ.ddmanager.campaign.dto.CampaignPlayerResponse;
import it.univ.ddmanager.campaign.model.CampaignPlayerStatus;
import it.univ.ddmanager.campaign.service.CampaignPlayerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CampaignPlayerController {

    private final CampaignPlayerService campaignPlayerService;

    @PreAuthorize("hasAnyRole('ADMIN','GM','PLAYER')")
    @PostMapping("/campaigns/{campaignId}/join-requests")
    public ResponseEntity<CampaignPlayerResponse> requestJoin(
        @PathVariable Long campaignId,
        @Valid @RequestBody CampaignPlayerRequest request
    ) {
        CampaignPlayerResponse response = campaignPlayerService.requestJoin(campaignId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @GetMapping("/campaigns/{campaignId}/join-requests")
    public List<CampaignPlayerResponse> listJoinRequests(
        @PathVariable Long campaignId,
        @RequestParam(name = "status", required = false) String status
    ) {
        return campaignPlayerService.listJoinRequestsForCampaign(campaignId, parseStatus(status));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PostMapping("/campaigns/{campaignId}/join-requests/{campaignPlayerId}/approve")
    public CampaignPlayerResponse approve(
        @PathVariable Long campaignId,
        @PathVariable Long campaignPlayerId
    ) {
        return campaignPlayerService.approve(campaignId, campaignPlayerId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PostMapping("/campaigns/{campaignId}/join-requests/{campaignPlayerId}/reject")
    public CampaignPlayerResponse reject(
        @PathVariable Long campaignId,
        @PathVariable Long campaignPlayerId
    ) {
        return campaignPlayerService.reject(campaignId, campaignPlayerId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM','PLAYER')")
    @GetMapping("/campaigns/{campaignId}/my-join-request")
    public CampaignPlayerResponse getMyJoinRequest(@PathVariable Long campaignId) {
        return campaignPlayerService.getMyJoinRequestForCampaign(campaignId);
    }

    @GetMapping("/campaigns/{campaignId}/players")
    public List<CampaignPlayerResponse> listApprovedPlayers(@PathVariable Long campaignId) {
        return campaignPlayerService.listApprovedPlayersForCampaign(campaignId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM','PLAYER')")
    @GetMapping("/campaign-players/my")
    public List<CampaignPlayerResponse> listMyCampaignPlayers() {
        return campaignPlayerService.listMyCampaignPlayers();
    }

    @PreAuthorize("hasRole('PLAYER')")
    @GetMapping("/join-requests/my")
    public List<CampaignPlayerResponse> listMyJoinRequests() {
        return campaignPlayerService.listMyJoinRequests();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @GetMapping("/dm/join-requests")
    public List<CampaignPlayerResponse> listPendingRequestsForGm() {
        return campaignPlayerService.listPendingRequestsForCurrentGm();
    }

    private CampaignPlayerStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return CampaignPlayerStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid campaign player status: " + raw);
        }
    }
}
