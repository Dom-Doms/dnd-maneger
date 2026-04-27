package it.univ.ddmanager.campaign.service;

import it.univ.ddmanager.campaign.dto.CampaignPlayerRequest;
import it.univ.ddmanager.campaign.dto.CampaignPlayerResponse;
import it.univ.ddmanager.campaign.model.Campaign;
import it.univ.ddmanager.campaign.model.CampaignPlayer;
import it.univ.ddmanager.campaign.model.CampaignPlayerStatus;
import it.univ.ddmanager.campaign.repository.CampaignPlayerRepository;
import it.univ.ddmanager.campaign.repository.CampaignRepository;
import it.univ.ddmanager.character.model.PlayerCharacter;
import it.univ.ddmanager.character.repository.PlayerCharacterRepository;
import it.univ.ddmanager.common.ConflictException;
import it.univ.ddmanager.common.ResourceNotFoundException;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.service.CurrentUserService;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CampaignPlayerService {

    private final CampaignRepository campaignRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final CampaignPlayerRepository campaignPlayerRepository;
    private final CurrentUserService currentUserService;

    public CampaignPlayerResponse requestJoin(Long campaignId, CampaignPlayerRequest request) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requirePlayableRole(currentUser);
        Campaign campaign = loadCampaign(campaignId);
        PlayerCharacter character = playerCharacterRepository.findById(Objects.requireNonNull(request.getCharacterId()))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Player character not found: " + request.getCharacterId()));
        if (!isOwner(character, currentUser)) {
            throw new AccessDeniedException("You can only use your own characters");
        }
        boolean alreadyParticipating = campaignPlayerRepository.existsByCampaignAndCharacterAndStatusIn(
                campaign,
                character,
                EnumSet.of(CampaignPlayerStatus.PENDING, CampaignPlayerStatus.APPROVED));
        if (alreadyParticipating) {
            throw new ConflictException("Character already requested or joined this campaign");
        }
        CampaignPlayer campaignPlayer = new CampaignPlayer();
        campaignPlayer.setCampaign(campaign);
        campaignPlayer.setCharacter(character);
        campaignPlayer.setPlayer(currentUser);
        campaignPlayer.setStatus(CampaignPlayerStatus.PENDING);
        campaignPlayer.setMessage(request.getMessage());
        campaignPlayer.setDecisionBy(null);
        CampaignPlayer saved = campaignPlayerRepository.save(Objects.requireNonNull(campaignPlayer));
        return CampaignPlayerResponse.fromEntity(saved);
    }

    public List<CampaignPlayerResponse> listJoinRequestsForCampaign(Long campaignId,
            CampaignPlayerStatus statusFilter) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        Campaign campaign = loadCampaign(campaignId);
        requireManageAccess(campaign, currentUser);
        List<CampaignPlayer> entries = statusFilter == null
                ? campaignPlayerRepository.findByCampaign(campaign)
                : campaignPlayerRepository.findByCampaignAndStatus(campaign, statusFilter);
        return entries.stream()
                .map(CampaignPlayerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public CampaignPlayerResponse approve(Long campaignId, Long campaignPlayerId) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        Campaign campaign = loadCampaign(campaignId);
        requireManageAccess(campaign, currentUser);
        CampaignPlayer campaignPlayer = campaignPlayerRepository.findByIdAndCampaign(campaignPlayerId, campaign)
                .orElseThrow(() -> new ResourceNotFoundException("Join request not found: " + campaignPlayerId));
        campaignPlayer.setStatus(CampaignPlayerStatus.APPROVED);
        campaignPlayer.setDecisionBy(currentUser);
        return CampaignPlayerResponse
                .fromEntity(campaignPlayerRepository.save(Objects.requireNonNull(campaignPlayer)));
    }

    public CampaignPlayerResponse reject(Long campaignId, Long campaignPlayerId) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        Campaign campaign = loadCampaign(campaignId);
        requireManageAccess(campaign, currentUser);
        CampaignPlayer campaignPlayer = campaignPlayerRepository.findByIdAndCampaign(campaignPlayerId, campaign)
                .orElseThrow(() -> new ResourceNotFoundException("Join request not found: " + campaignPlayerId));
        campaignPlayer.setStatus(CampaignPlayerStatus.REJECTED);
        campaignPlayer.setDecisionBy(currentUser);
        return CampaignPlayerResponse.fromEntity(campaignPlayerRepository.save(Objects.requireNonNull(campaignPlayer)));
    }

    public CampaignPlayerResponse getMyJoinRequestForCampaign(Long campaignId) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        Campaign campaign = loadCampaign(campaignId);
        CampaignPlayer campaignPlayer = campaignPlayerRepository.findByCampaignAndPlayer(campaign, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Join request not found for campaign: " + campaignId));
        return CampaignPlayerResponse.fromEntity(campaignPlayer);
    }

    public List<CampaignPlayerResponse> listMyCampaignPlayers() {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requirePlayableRole(currentUser);
        return campaignPlayerRepository.findByPlayer(currentUser).stream()
                .map(CampaignPlayerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<CampaignPlayerResponse> listMyJoinRequests() {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requirePlayableRole(currentUser);
        return campaignPlayerRepository.findByPlayer(currentUser).stream()
                .sorted(Comparator.comparing(CampaignPlayer::getId).reversed())
                .map(CampaignPlayerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<CampaignPlayerResponse> listPendingRequestsForCurrentGm() {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        return listPendingRequestsForGm(currentUser);
    }

    public List<CampaignPlayerResponse> listPendingRequestsForGm(User gm) {
        if (!isGmOrAdmin(gm)) {
            throw new AccessDeniedException("Access is denied");
        }
        List<CampaignPlayer> entries = isAdmin(gm)
                ? campaignPlayerRepository.findByStatus(CampaignPlayerStatus.PENDING)
                : campaignPlayerRepository.findByCampaignOwnerIdAndStatus(gm.getId(), CampaignPlayerStatus.PENDING);
        return entries.stream()
                .sorted(Comparator.comparing(CampaignPlayer::getId).reversed())
                .map(CampaignPlayerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<CampaignPlayerResponse> listApprovedPlayersForCampaign(Long campaignId) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        Campaign campaign = loadCampaign(campaignId);
        return campaignPlayerRepository.findByCampaignAndStatus(campaign, CampaignPlayerStatus.APPROVED).stream()
                .filter(entry -> canViewCharacter(entry.getCharacter(), currentUser))
                .map(CampaignPlayerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private Campaign loadCampaign(Long campaignId) {
        return campaignRepository.findById(Objects.requireNonNull(campaignId))
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
    }

    private void requireManageAccess(Campaign campaign, User currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (campaign.getOwner() != null && campaign.getOwner().getId() != null
                && campaign.getOwner().getId().equals(currentUser.getId())) {
            return;
        }
        throw new AccessDeniedException("Access is denied");
    }

    private boolean isOwner(PlayerCharacter character, User user) {
        return character.getOwner() != null
                && character.getOwner().getId() != null
                && character.getOwner().getId().equals(user.getId());
    }

    private boolean canViewCharacter(PlayerCharacter character, User user) {
        if (user == null) {
            return character.isVisibleToPlayers();
        }
        if (isOwner(character, user) || isGmOrAdmin(user)) {
            return true;
        }
        return character.isVisibleToPlayers();
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private boolean isGmOrAdmin(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(role -> "ROLE_GM".equals(role) || "ROLE_ADMIN".equals(role));
    }

    private void requirePlayableRole(User user) {
        if (!hasPlayableRole(user)) {
            throw new AccessDeniedException("Only players can perform this action");
        }
    }

    private boolean hasPlayableRole(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(role -> "ROLE_PLAYER".equals(role) || "ROLE_GM".equals(role) || "ROLE_ADMIN".equals(role));
    }
}
