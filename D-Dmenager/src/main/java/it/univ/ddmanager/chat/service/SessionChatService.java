package it.univ.ddmanager.chat.service;

import it.univ.ddmanager.campaign.model.Campaign;
import it.univ.ddmanager.campaign.model.CampaignPlayer;
import it.univ.ddmanager.campaign.model.CampaignPlayerStatus;
import it.univ.ddmanager.campaign.model.Session;
import it.univ.ddmanager.campaign.repository.CampaignPlayerRepository;
import it.univ.ddmanager.campaign.repository.SessionRepository;
import it.univ.ddmanager.chat.dto.SessionChatMessageRequest;
import it.univ.ddmanager.chat.dto.SessionChatMessageResponse;
import it.univ.ddmanager.chat.model.SessionChatMessage;
import it.univ.ddmanager.chat.repository.SessionChatMessageRepository;
import it.univ.ddmanager.character.model.CharacterLanguage;
import it.univ.ddmanager.character.model.PlayerCharacter;
import it.univ.ddmanager.character.repository.PlayerCharacterRepository;
import it.univ.ddmanager.common.ResourceNotFoundException;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.repository.UserRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionChatService {

    private final SessionRepository sessionRepository;
    private final SessionChatMessageRepository chatMessageRepository;
    private final CampaignPlayerRepository campaignPlayerRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final UserRepository userRepository;
    private final LanguageObfuscator languageObfuscator;

    private final ConcurrentMap<Long, ReentrantReadWriteLock> sessionLocks = new ConcurrentHashMap<>();

    @Transactional
    public SessionChatMessageResponse sendMessage(Long sessionId, SessionChatMessageRequest request, User currentUser) {
        ReentrantReadWriteLock lock = obtainLock(sessionId);
        lock.writeLock().lock();
        try {
            Session session = loadSession(sessionId);
            Campaign campaign = session.getCampaign();
            ParticipationContext context = resolveParticipationContext(campaign, currentUser);
            CharacterLanguage language = parseLanguage(request.getLanguage());
            String rawContent = request.getContent();
            if (rawContent == null) {
                throw new IllegalArgumentException("Message content cannot be empty");
            }
            String content = rawContent.trim();
            if (content.isEmpty()) {
                throw new IllegalArgumentException("Message content cannot be empty");
            }

            User recipientUser = null;
            if (request.getRecipientUserId() != null) {
                recipientUser = resolveRecipientUser(request.getRecipientUserId(), campaign);
            }

            PlayerCharacter senderCharacter = resolveSenderCharacter(request.getSenderCharacterId(), context, campaign,
                    currentUser, language);

            SessionChatMessage message = new SessionChatMessage();
            message.setSession(session);
            message.setSenderUser(currentUser);
            message.setRecipientUser(recipientUser);
            message.setSenderCharacter(senderCharacter);
            message.setLanguage(language);
            message.setContentRaw(content);
            message.setMessageType(request.getMessageType());

            SessionChatMessage saved = chatMessageRepository.save(message);
            return buildResponse(saved, context);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Transactional(readOnly = true)
    public List<SessionChatMessageResponse> listMessages(Long sessionId, User currentUser, Long recipientUserId,
            Instant from) {
        ReentrantReadWriteLock lock = obtainLock(sessionId);
        lock.readLock().lock();
        try {
            Session session = loadSession(sessionId);
            Campaign campaign = session.getCampaign();
            ParticipationContext context = resolveParticipationContext(campaign, currentUser);

            List<SessionChatMessage> messages;
            if (recipientUserId != null) {
                User recipient = resolveRecipientUser(recipientUserId, campaign);
                if (from == null) {
                    messages = chatMessageRepository.findPrivateMessages(session, currentUser, recipient);
                } else {
                    messages = chatMessageRepository.findPrivateMessagesAfter(session, currentUser, recipient, from);
                }
            } else {
                if (from == null) {
                    messages = chatMessageRepository.findBySessionAndRecipientUserIsNullOrderByCreatedAtAsc(session);
                } else {
                    messages = chatMessageRepository
                            .findBySessionAndRecipientUserIsNullAndCreatedAtAfterOrderByCreatedAtAsc(session, from);
                }
            }

            return messages.stream()
                    .map(message -> buildResponse(message, context))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    private User resolveRecipientUser(Long recipientUserId, Campaign campaign) {
        User recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found"));

        // Ensure recipient is part of the campaign (Owner, Player, or Admin/Global
        // logic if needed)
        // For simplicity, checking if they are the owner or have a player record.
        boolean isOwner = isCampaignOwner(campaign, recipient);
        boolean isPlayer = campaignPlayerRepository
                .findByCampaignAndPlayerAndStatus(campaign, recipient, CampaignPlayerStatus.APPROVED).isPresent();

        if (!isOwner && !isPlayer) {
            throw new IllegalArgumentException("Recipient is not part of this campaign");
        }
        return recipient;
    }

    private Session loadSession(Long sessionId) {
        return sessionRepository.findById(Objects.requireNonNull(sessionId, "Session ID must not be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
    }

    private ParticipationContext resolveParticipationContext(Campaign campaign, User currentUser) {
        if (campaign == null) {
            throw new ResourceNotFoundException("Campaign not found for session");
        }
        if (isAdmin(currentUser) || isCampaignOwner(campaign, currentUser)) {
            return ParticipationContext.moderator();
        }
        CampaignPlayer membership = campaignPlayerRepository
                .findByCampaignAndPlayerAndStatus(campaign, currentUser, CampaignPlayerStatus.APPROVED)
                .orElseThrow(() -> new AccessDeniedException("You are not part of this campaign"));
        PlayerCharacter activeCharacter = membership.getCharacter();
        if (activeCharacter == null) {
            throw new AccessDeniedException("No character assigned to this campaign");
        }
        return ParticipationContext.player(activeCharacter, languagesOrDefault(activeCharacter));
    }

    private PlayerCharacter resolveSenderCharacter(
            Long senderCharacterId,
            ParticipationContext context,
            Campaign campaign,
            User currentUser,
            CharacterLanguage language) {
        if (!context.isModerator()) {
            if (senderCharacterId == null) {
                throw new IllegalArgumentException("senderCharacterId is required for players");
            }
            PlayerCharacter activeCharacter = context.getActiveCharacter();
            if (!Objects.equals(activeCharacter.getId(), senderCharacterId)) {
                throw new AccessDeniedException("You can only speak as your approved character");
            }
            if (!knowsLanguage(activeCharacter, language)) {
                throw new IllegalArgumentException("Character does not know " + language.name());
            }
            return activeCharacter;
        }
        if (senderCharacterId == null) {
            return null;
        }
        PlayerCharacter character = playerCharacterRepository.findById(senderCharacterId)
                .orElseThrow(() -> new ResourceNotFoundException("Player character not found: " + senderCharacterId));

        // DM Security Check: DM can strictly ONLY use characters they own (or if they
        // are Admin)
        // This prevents DM from impersonating a player's PC.
        if (!isOwner(character, currentUser) && !isAdmin(currentUser)) {
            throw new AccessDeniedException("You can only use characters you own");
        }

        // For Moderator/DM, we do NOT require the character to be "Approved" in the
        // campaign.
        // This allows the DM to use their own characters (NPCs created as
        // PlayerCharacter) without joining the campaign.

        if (!knowsLanguage(character, language)) {
            throw new IllegalArgumentException("Character does not know " + language.name());
        }
        return character;
    }

    private SessionChatMessageResponse buildResponse(SessionChatMessage message, ParticipationContext context) {
        String visibleContent;
        if (context.isModerator()) {
            visibleContent = message.getContentRaw();
        } else if (context.getVisibleLanguages().contains(message.getLanguage())) {
            visibleContent = message.getContentRaw();
        } else {
            visibleContent = languageObfuscator.encode(message.getLanguage(), message.getContentRaw());
        }
        return SessionChatMessageResponse.fromEntity(message, visibleContent);
    }

    private boolean knowsLanguage(PlayerCharacter character, CharacterLanguage language) {
        if (language == null || language == CharacterLanguage.COMMON) {
            return true;
        }
        return languagesOrDefault(character).contains(language);
    }

    private Set<CharacterLanguage> languagesOrDefault(PlayerCharacter character) {
        if (character.getKnownLanguages() == null || character.getKnownLanguages().isEmpty()) {
            return EnumSet.of(CharacterLanguage.COMMON);
        }
        EnumSet<CharacterLanguage> languages = EnumSet.copyOf(character.getKnownLanguages());
        languages.add(CharacterLanguage.COMMON);
        return languages;
    }

    private boolean isCampaignOwner(Campaign campaign, User user) {
        return campaign.getOwner() != null
                && campaign.getOwner().getId() != null
                && campaign.getOwner().getId().equals(user.getId());
    }

    private boolean isOwner(PlayerCharacter character, User user) {
        return character.getOwner() != null
                && character.getOwner().getId() != null
                && character.getOwner().getId().equals(user.getId());
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private CharacterLanguage parseLanguage(String requestedLanguage) {
        if (requestedLanguage == null || requestedLanguage.isBlank()) {
            return CharacterLanguage.COMMON;
        }
        try {
            // Handle space vs underscore (e.g. "Thieves' Cant", "Deep Speech")
            String normalized = requestedLanguage.trim()
                    .toUpperCase()
                    .replace(" ", "_")
                    .replace("'", "") // Remove apostrophes (Thieves' Cant -> THIEVES CANT -> THIEVES_CANT)
                    .replace("-", "_");

            return CharacterLanguage.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            // Log warning?
            return CharacterLanguage.COMMON;
        }
    }

    private ReentrantReadWriteLock obtainLock(Long sessionId) {
        return sessionLocks.computeIfAbsent(sessionId, id -> new ReentrantReadWriteLock());
    }

    private static final class ParticipationContext {
        private final boolean moderator;
        private final PlayerCharacter activeCharacter;
        private final Set<CharacterLanguage> visibleLanguages;

        private ParticipationContext(boolean moderator, PlayerCharacter activeCharacter,
                Set<CharacterLanguage> visibleLanguages) {
            this.moderator = moderator;
            this.activeCharacter = activeCharacter;
            this.visibleLanguages = visibleLanguages;
        }

        static ParticipationContext moderator() {
            return new ParticipationContext(true, null, EnumSet.allOf(CharacterLanguage.class));
        }

        static ParticipationContext player(PlayerCharacter character, Set<CharacterLanguage> languages) {
            return new ParticipationContext(false, character, EnumSet.copyOf(languages));
        }

        boolean isModerator() {
            return moderator;
        }

        PlayerCharacter getActiveCharacter() {
            return activeCharacter;
        }

        Set<CharacterLanguage> getVisibleLanguages() {
            return visibleLanguages;
        }
    }
}