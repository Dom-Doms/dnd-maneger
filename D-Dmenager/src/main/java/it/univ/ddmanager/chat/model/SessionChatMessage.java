package it.univ.ddmanager.chat.model;

import it.univ.ddmanager.campaign.model.Session;
import it.univ.ddmanager.character.model.CharacterLanguage;
import it.univ.ddmanager.character.model.PlayerCharacter;
import it.univ.ddmanager.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "session_chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class SessionChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_character_id")
    private PlayerCharacter senderCharacter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User senderUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    private User recipientUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CharacterLanguage language;

    @Column(name = "content_raw", nullable = false, columnDefinition = "text")
    private String contentRaw;

    @Column(name = "message_type")
    private String messageType;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamp(6) with time zone default now()")
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
