package it.univ.ddmanager.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStats {

    private long worldCount;
    private long campaignCount;
    private long sessionCount;
    private long npcCount;
    private long locationCount;
    private long itemCount;
    private long playerCharacterCount;
    private long sessionEventCount;
    private long pendingJoinRequests;
}
