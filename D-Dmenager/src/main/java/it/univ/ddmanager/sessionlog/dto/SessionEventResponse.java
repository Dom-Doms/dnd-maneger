// src/main/java/it/univ/ddmanager/sessionlog/dto/SessionEventResponse.java
package it.univ.ddmanager.sessionlog.dto;

import it.univ.ddmanager.sessionlog.model.SessionEvent;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionEventResponse {

    private Long id;
    private Long sessionId;
    private Long ownerId;
    private String ownerNickname;
    private String title;
    private String type;
    private String description;
    private String inGameTime;
    private Boolean isVisibleToPlayers;
    private Instant createdAt;

    public static SessionEventResponse fromEntityForGm(SessionEvent event) {
        return new SessionEventResponse(
            event.getId(),
            event.getSession() != null ? event.getSession().getId() : null,
            event.getOwner() != null ? event.getOwner().getId() : null,
            event.getOwner() != null ? event.getOwner().getNickname() : null,
            event.getTitle(),
            event.getType(),
            event.getDescription(),
            event.getInGameTime(),
            event.isVisibleToPlayers(),
            event.getCreatedAt()
        );
    }

    public static SessionEventResponse fromEntityForPlayer(SessionEvent event) {
        return new SessionEventResponse(
            event.getId(),
            event.getSession() != null ? event.getSession().getId() : null,
            event.getOwner() != null ? event.getOwner().getId() : null,
            event.getOwner() != null ? event.getOwner().getNickname() : null,
            event.getTitle(),
            event.getType(),
            event.getDescription(),
            event.getInGameTime(),
            event.isVisibleToPlayers(),
            event.getCreatedAt()
        );
    }
}
