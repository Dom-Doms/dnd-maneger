// src/main/java/it/univ/ddmanager/campaign/service/SessionService.java
package it.univ.ddmanager.campaign.service;

import it.univ.ddmanager.campaign.dto.SessionRequest;
import it.univ.ddmanager.campaign.model.Campaign;
import it.univ.ddmanager.campaign.model.Session;
import it.univ.ddmanager.campaign.repository.CampaignRepository;
import it.univ.ddmanager.campaign.repository.SessionRepository;
import it.univ.ddmanager.common.ResourceNotFoundException;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.service.CurrentUserService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final CampaignRepository campaignRepository;
    private final CurrentUserService currentUserService;

    public Session createSession(Long campaignId, SessionRequest request) {
        Campaign campaign = campaignRepository.findById(Objects.requireNonNull(campaignId))
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));

        User owner = currentUserService.getCurrentUserOrThrow();
        requireCampaignManagementPermission(campaign, owner);

        Session session = new Session();
        session.setTitle(request.getTitle());
        session.setSessionNumber(request.getSessionNumber());
        session.setSessionDate(request.getSessionDate());
        session.setNotes(request.getNotes());
        session.setCampaign(campaign);
        session.setOwner(owner);

        return sessionRepository.save(Objects.requireNonNull(session));
    }

    @Transactional(readOnly = true)
    public Session getSessionById(Long sessionId) {
        return sessionRepository.findById(Objects.requireNonNull(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
    }

    @Transactional
    public Session updateSession(Long sessionId, SessionRequest request) {
        Session session = getSessionById(sessionId);
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requireCampaignManagementPermission(session.getCampaign(), currentUser);

        session.setTitle(request.getTitle());
        session.setSessionNumber(request.getSessionNumber());
        session.setSessionDate(request.getSessionDate());
        session.setNotes(request.getNotes());

        return sessionRepository.save(Objects.requireNonNull(session));
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        Session session = getSessionById(sessionId);
        User currentUser = currentUserService.getCurrentUserOrThrow();
        requireCampaignManagementPermission(session.getCampaign(), currentUser);
        sessionRepository.delete(Objects.requireNonNull(session));
    }

    @Transactional(readOnly = true)
    public List<Session> getSessionsByCampaign(Long campaignId) {
        return sessionRepository.findByCampaignId(campaignId);
    }

    @Transactional(readOnly = true)
    public List<Session> getMySessions() {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        return sessionRepository.findSessionsForUser(currentUser.getId());
    }

    private void requireCampaignManagementPermission(Campaign campaign, User user) {
        if (user == null) {
            throw new AccessDeniedException("Access is denied");
        }
        boolean isAdmin = hasRole(user, "ROLE_ADMIN");
        boolean isCampaignOwner = campaign != null
                && campaign.getOwner() != null
                && campaign.getOwner().getId() != null
                && campaign.getOwner().getId().equals(user.getId());
        boolean isGmOwner = hasRole(user, "ROLE_GM") && isCampaignOwner;
        if (!isAdmin && !isGmOwner) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> roleName.equals(role.getName()));
    }
}
