package it.univ.ddmanager.dashboard.service;

import it.univ.ddmanager.campaign.dto.CampaignPlayerResponse;
import it.univ.ddmanager.campaign.dto.CampaignResponse;
import it.univ.ddmanager.campaign.repository.SessionRepository;
import it.univ.ddmanager.campaign.service.CampaignService;
import it.univ.ddmanager.campaign.service.CampaignPlayerService;
import it.univ.ddmanager.character.dto.PlayerCharacterResponse;
import it.univ.ddmanager.character.repository.PlayerCharacterRepository;
import it.univ.ddmanager.dashboard.dto.DashboardResponse;
import it.univ.ddmanager.dashboard.dto.DashboardStats;
import it.univ.ddmanager.dashboard.dto.DashboardView;
import it.univ.ddmanager.item.dto.ItemResponse;
import it.univ.ddmanager.item.model.Item;
import it.univ.ddmanager.item.service.ItemService;
import it.univ.ddmanager.location.dto.LocationResponse;
import it.univ.ddmanager.location.model.Location;
import it.univ.ddmanager.location.service.LocationService;
import it.univ.ddmanager.npc.dto.NpcResponse;
import it.univ.ddmanager.npc.service.NpcService;
import it.univ.ddmanager.sessionlog.dto.SessionEventResponse;
import it.univ.ddmanager.sessionlog.repository.SessionEventRepository;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.service.CurrentUserService;
import it.univ.ddmanager.world.dto.WorldResponse;
import it.univ.ddmanager.world.service.WorldService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int PENDING_REQUEST_LIMIT = 10;

    private final CurrentUserService currentUserService;
    private final WorldService worldService;
    private final CampaignService campaignService;
    private final CampaignPlayerService campaignPlayerService;
    private final NpcService npcService;
    private final LocationService locationService;
    private final ItemService itemService;
    private final SessionRepository sessionRepository;
    private final SessionEventRepository sessionEventRepository;
    private final PlayerCharacterRepository playerCharacterRepository;

    public DashboardResponse buildDashboard() {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        boolean gmView = isGmOrAdmin(currentUser);

        List<WorldResponse> worlds = worldService.getAllWorlds().stream()
                .map(worldService::toResponse)
                .collect(Collectors.toList());

        List<CampaignResponse> campaigns = campaignService.getAllCampaigns().stream()
                .map(CampaignResponse::fromEntity)
                .collect(Collectors.toList());

        List<NpcResponse> npcs = npcService.getAllForUser(currentUser).stream()
                .map(npc -> NpcResponse.fromEntity(npc, currentUser))
                .collect(Collectors.toList());

        List<LocationResponse> locations = locationService.getAllForUser(currentUser).stream()
                .map(location -> mapLocation(location, gmView))
                .collect(Collectors.toList());

        List<ItemResponse> items = itemService.getAllForUser(currentUser).stream()
                .map(item -> mapItem(item, gmView))
                .collect(Collectors.toList());

        List<PlayerCharacterResponse> myCharacters = playerCharacterRepository.findByOwner(currentUser).stream()
                .map(character -> PlayerCharacterResponse.fromEntity(character, currentUser))
                .collect(Collectors.toList());

        List<SessionEventResponse> recentEvents = gmView
                ? sessionEventRepository.findTop10ByOrderByCreatedAtDesc().stream()
                        .map(SessionEventResponse::fromEntityForGm)
                        .collect(Collectors.toList())
                : sessionEventRepository.findTop10ByIsVisibleToPlayersTrueOrderByCreatedAtDesc().stream()
                        .map(SessionEventResponse::fromEntityForPlayer)
                        .collect(Collectors.toList());

        List<CampaignPlayerResponse> pendingJoinRequests = gmView
                ? loadPendingJoinRequests(currentUser)
                : List.of();

        DashboardStats stats = DashboardStats.builder()
                .worldCount(worlds.size())
                .campaignCount(campaigns.size())
                .sessionCount(sessionRepository.count())
                .npcCount(npcs.size())
                .locationCount(locations.size())
                .itemCount(items.size())
                .playerCharacterCount(gmView ? playerCharacterRepository.count() : myCharacters.size())
                .sessionEventCount(gmView
                        ? sessionEventRepository.count()
                        : sessionEventRepository.countByIsVisibleToPlayersTrue())
                .pendingJoinRequests(pendingJoinRequests.size())
                .build();

        return DashboardResponse.builder()
                .view(gmView ? DashboardView.GM : DashboardView.PLAYER)
                .stats(stats)
                .worlds(worlds)
                .campaigns(campaigns)
                .npcs(npcs)
                .locations(locations)
                .items(items)
                .recentEvents(recentEvents)
                .pendingJoinRequests(pendingJoinRequests)
                .myCharacters(myCharacters)
                .build();
    }

    private List<CampaignPlayerResponse> loadPendingJoinRequests(User currentUser) {
        return campaignPlayerService.listPendingRequestsForGm(currentUser).stream()
                .limit(PENDING_REQUEST_LIMIT)
                .collect(Collectors.toList());
    }

    private LocationResponse mapLocation(Location location, boolean gmView) {
        return gmView ? LocationResponse.fromEntityForGm(location) : LocationResponse.fromEntityForPlayer(location);
    }

    private ItemResponse mapItem(Item item, boolean gmView) {
        return gmView ? ItemResponse.fromEntityForGm(item) : ItemResponse.fromEntityForPlayer(item);
    }

    private boolean isGmOrAdmin(User user) {
        return hasRole(user, "ROLE_ADMIN") || hasRole(user, "ROLE_GM");
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(roleName::equals);
    }
}
