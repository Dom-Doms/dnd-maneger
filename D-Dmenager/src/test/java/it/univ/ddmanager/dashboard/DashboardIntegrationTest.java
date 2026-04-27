package it.univ.ddmanager.dashboard;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.univ.ddmanager.support.TestAuthenticationHelper;
import it.univ.ddmanager.support.TestDatabaseCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DashboardIntegrationTest {

    private static final String GM_EMAIL = "gm_dashboard@example.com";
    private static final String GM_PASSWORD = "GmDash123!";
    private static final String PLAYER_EMAIL = "player_dashboard@example.com";
    private static final String PLAYER_PASSWORD = "PlayerDash123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDatabaseCleanup databaseCleanup;

    @Autowired
    private TestAuthenticationHelper authHelper;

    private long worldId;
    private long campaignId;
    private long sessionId;

    @BeforeEach
    void setUp() throws Exception {
        databaseCleanup.truncateAll();
        registerUser(GM_EMAIL, GM_PASSWORD, "DashboardGM", "DM");
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "DashboardPlayer", "PLAYER");

        worldId = createWorld("Dashboard World");
        campaignId = createCampaign(worldId, "Dashboard Campaign");
        sessionId = createSession(campaignId, "Dashboard Session");

        createNpc(worldId, "Visible NPC", true, "GM info visible");
        createNpc(worldId, "Hidden NPC", false, "GM secret");

        createLocation(worldId, "Visible Location", true, "GM location notes");
        createLocation(worldId, "Hidden Location", false, "GM hidden location");

        createItem(worldId, "Visible Item", true, "GM item notes");
        createItem(worldId, "Hidden Item", false, "GM hidden loot");

        createSessionEvent(sessionId, "Visible Event", true);
        createSessionEvent(sessionId, "Hidden Event", false);

        long characterId = createPlayerCharacter(worldId, "Hero Player");
        createJoinRequest(campaignId, characterId);
    }

    @Test
    void dashboard_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboard_asGmShowsCompleteData() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.view").value("GM"))
            .andExpect(jsonPath("$.worlds.length()").value(1))
            .andExpect(jsonPath("$.campaigns.length()").value(1))
            .andExpect(jsonPath("$.npcs.length()").value(2))
            .andExpect(jsonPath("$.npcs[?(@.name=='Hidden NPC')].gmNotes").value(hasItem("GM secret")))
            .andExpect(jsonPath("$.locations.length()").value(2))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.recentEvents.length()").value(2))
            .andExpect(jsonPath("$.pendingJoinRequests.length()").value(1))
            .andExpect(jsonPath("$.stats.pendingJoinRequests").value(1))
            .andExpect(jsonPath("$.stats.playerCharacterCount").value(1))
            .andExpect(jsonPath("$.stats.sessionEventCount").value(2))
            .andExpect(jsonPath("$.myCharacters.length()").value(0));
    }

    @Test
    void dashboard_asPlayerFiltersPrivateData() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.view").value("PLAYER"))
            .andExpect(jsonPath("$.npcs.length()").value(1))
            .andExpect(jsonPath("$.npcs[0].gmNotes").value(nullValue()))
            .andExpect(jsonPath("$.locations.length()").value(1))
            .andExpect(jsonPath("$.locations[0].gmNotes").value(nullValue()))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].gmNotes").value(nullValue()))
            .andExpect(jsonPath("$.recentEvents.length()").value(1))
            .andExpect(jsonPath("$.pendingJoinRequests").value(empty()))
            .andExpect(jsonPath("$.stats.pendingJoinRequests").value(0))
            .andExpect(jsonPath("$.stats.playerCharacterCount").value(1))
            .andExpect(jsonPath("$.stats.sessionEventCount").value(1))
            .andExpect(jsonPath("$.myCharacters.length()").value(1))
            .andExpect(jsonPath("$.npcs[0].isVisibleToPlayers").value(true));
    }

    private void registerUser(String email, String password, String nickname, String role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s","nickname":"%s","role":"%s"}
                    """.formatted(email, password, nickname, role)))
            .andExpect(status().isCreated());
    }

    private long createWorld(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/worlds")
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"%s","description":"World for dashboard"}
                    """.formatted(name)))
            .andExpect(status().isCreated())
            .andReturn();
        return parseId(result);
    }

    private long createCampaign(long createdWorldId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns")
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"worldId":%d,"name":"%s","description":"Campaign dashboard","status":"ACTIVE"}
                    """.formatted(createdWorldId, name)))
            .andExpect(status().isCreated())
            .andReturn();
        return parseId(result);
    }

    private long createSession(long createdCampaignId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns/{campaignId}/sessions", createdCampaignId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"%s","sessionNumber":1,"sessionDate":"2024-05-05","notes":"Dashboard session"}
                    """.formatted(title)))
            .andExpect(status().isCreated())
            .andReturn();
        return parseId(result);
    }

    private void createNpc(long createdWorldId, String name, boolean visible, String gmNotes) throws Exception {
        mockMvc.perform(post("/api/npcs")
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "worldId":%d,
                        "name":"%s",
                        "description":"NPC %s",
                        "gmNotes":"%s",
                        "isVisibleToPlayers":%s
                    }
                    """.formatted(createdWorldId, name, name, gmNotes, visible)))
            .andExpect(status().isCreated());
    }

    private void createLocation(long createdWorldId, String name, boolean visible, String gmNotes) throws Exception {
        mockMvc.perform(post("/api/locations")
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "worldId":%d,
                        "name":"%s",
                        "description":"Location %s",
                        "gmNotes":"%s",
                        "isVisibleToPlayers":%s
                    }
                    """.formatted(createdWorldId, name, name, gmNotes, visible)))
            .andExpect(status().isCreated());
    }

    private void createItem(long createdWorldId, String name, boolean visible, String gmNotes) throws Exception {
        mockMvc.perform(post("/api/items")
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "worldId":%d,
                        "name":"%s",
                        "type":"Wondrous",
                        "description":"Item %s",
                        "gmNotes":"%s",
                        "isVisibleToPlayers":%s
                    }
                    """.formatted(createdWorldId, name, name, gmNotes, visible)))
            .andExpect(status().isCreated());
    }

    private void createSessionEvent(long createdSessionId, String title, boolean visible) throws Exception {
        mockMvc.perform(post("/api/session-events")
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "sessionId":%d,
                        "title":"%s",
                        "description":"Event %s",
                        "isVisibleToPlayers":%s
                    }
                    """.formatted(createdSessionId, title, title, visible)))
            .andExpect(status().isCreated());
    }

    private long createPlayerCharacter(long createdWorldId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/characters")
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "worldId":%d,
                        "name":"%s",
                        "race":"Human",
                        "characterClass":"Wizard",
                        "level":3,
                        "experiencePoints":900,
                        "strength":10,
                        "dexterity":14,
                        "constitution":12,
                        "intelligence":15,
                        "wisdom":13,
                        "charisma":11,
                        "maxHitPoints":20,
                        "currentHitPoints":18,
                        "armorClass":14,
                        "speed":30,
                        "isVisibleToPlayers":true,
                        "gmNotes":"Character secrets"
                    }
                    """.formatted(createdWorldId, name)))
            .andExpect(status().isCreated())
            .andReturn();
        return parseId(result);
    }

    private void createJoinRequest(long createdCampaignId, long characterId) throws Exception {
        mockMvc.perform(post("/api/campaigns/{campaignId}/join-requests", createdCampaignId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"characterId":%d}
                    """.formatted(characterId)))
            .andExpect(status().isCreated());
    }

    private long parseId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
