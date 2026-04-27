package it.univ.ddmanager.dashboard.dto;

import it.univ.ddmanager.campaign.dto.CampaignPlayerResponse;
import it.univ.ddmanager.campaign.dto.CampaignResponse;
import it.univ.ddmanager.character.dto.PlayerCharacterResponse;
import it.univ.ddmanager.item.dto.ItemResponse;
import it.univ.ddmanager.location.dto.LocationResponse;
import it.univ.ddmanager.npc.dto.NpcResponse;
import it.univ.ddmanager.sessionlog.dto.SessionEventResponse;
import it.univ.ddmanager.world.dto.WorldResponse;
import java.util.List;
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
public class DashboardResponse {

    private DashboardView view;
    private DashboardStats stats;
    private List<WorldResponse> worlds;
    private List<CampaignResponse> campaigns;
    private List<NpcResponse> npcs;
    private List<LocationResponse> locations;
    private List<ItemResponse> items;
    private List<SessionEventResponse> recentEvents;
    private List<CampaignPlayerResponse> pendingJoinRequests;
    private List<PlayerCharacterResponse> myCharacters;
}
