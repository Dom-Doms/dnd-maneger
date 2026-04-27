package it.univ.ddmanager.chat.dto;

import it.univ.ddmanager.chat.model.SessionChatMessage;
import it.univ.ddmanager.character.model.PlayerCharacter;
import it.univ.ddmanager.user.model.User;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SessionChatMessageResponse {

    private final Long id;
    private final Long sessionId;
    private final Long senderUserId;
    private final String senderNickname;
    private final Long senderCharacterId;
    private final String senderCharacterName;
    private final String language;
    private final String contentVisible;
    private final String messageType;
    private final Instant createdAt;
    private final Long recipientUserId;

    public static SessionChatMessageResponse fromEntity(SessionChatMessage message, String visibleContent) {
        User sender = message.getSenderUser();
        User recipient = message.getRecipientUser();
        PlayerCharacter character = message.getSenderCharacter();
        return SessionChatMessageResponse.builder()
                .id(message.getId())
                .sessionId(message.getSession() != null ? message.getSession().getId() : null)
                .senderUserId(sender != null ? sender.getId() : null)
                .senderNickname(sender != null ? sender.getNickname() : null)
                .senderCharacterId(character != null ? character.getId() : null)
                .senderCharacterName(character != null ? character.getName() : null)
                .language(message.getLanguage() != null ? message.getLanguage().name() : null)
                .contentVisible(visibleContent)
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .recipientUserId(recipient != null ? recipient.getId() : null)
                .build();
    }
}
