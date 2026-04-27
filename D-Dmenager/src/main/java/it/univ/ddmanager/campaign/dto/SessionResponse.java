// src/main/java/it/univ/ddmanager/campaign/dto/SessionResponse.java
package it.univ.ddmanager.campaign.dto;

import it.univ.ddmanager.campaign.model.Session;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    private Long id;
    private String title;
    private Integer sessionNumber;
    private LocalDate sessionDate;
    private String notes;
    private Long campaignId;
    private Long ownerId;
    private String ownerNickname;

    public static SessionResponse fromEntity(Session session) {
        Long ownerId = session.getOwner() != null ? session.getOwner().getId() : null;
        String ownerNickname = session.getOwner() != null ? session.getOwner().getNickname() : null;
        return new SessionResponse(
            session.getId(),
            session.getTitle(),
            session.getSessionNumber(),
            session.getSessionDate(),
            session.getNotes(),
            session.getCampaign() != null ? session.getCampaign().getId() : null,
            ownerId,
            ownerNickname
        );
    }
}
