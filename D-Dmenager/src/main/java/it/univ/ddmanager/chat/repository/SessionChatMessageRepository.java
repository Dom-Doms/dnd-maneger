package it.univ.ddmanager.chat.repository;

import it.univ.ddmanager.campaign.model.Session;
import it.univ.ddmanager.chat.model.SessionChatMessage;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionChatMessageRepository extends JpaRepository<SessionChatMessage, Long> {

    List<SessionChatMessage> findBySessionAndRecipientUserIsNullOrderByCreatedAtAsc(Session session);

    List<SessionChatMessage> findBySessionAndRecipientUserIsNullAndCreatedAtAfterOrderByCreatedAtAsc(Session session,
            Instant createdAt);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM SessionChatMessage m WHERE m.session = :session AND ("
            +
            "(m.senderUser = :user1 AND m.recipientUser = :user2) OR " +
            "(m.senderUser = :user2 AND m.recipientUser = :user1)) " +
            "ORDER BY m.createdAt ASC")
    List<SessionChatMessage> findPrivateMessages(Session session, it.univ.ddmanager.user.model.User user1,
            it.univ.ddmanager.user.model.User user2);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM SessionChatMessage m WHERE m.session = :session AND ("
            +
            "(m.senderUser = :user1 AND m.recipientUser = :user2) OR " +
            "(m.senderUser = :user2 AND m.recipientUser = :user1)) " +
            "AND m.createdAt > :from ORDER BY m.createdAt ASC")
    List<SessionChatMessage> findPrivateMessagesAfter(Session session, it.univ.ddmanager.user.model.User user1,
            it.univ.ddmanager.user.model.User user2, Instant from);
}
