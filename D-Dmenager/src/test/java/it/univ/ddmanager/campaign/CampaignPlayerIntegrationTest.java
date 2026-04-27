package it.univ.ddmanager.campaign;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.univ.ddmanager.support.TestAuthenticationHelper;
import it.univ.ddmanager.support.TestDatabaseCleanup;
import it.univ.ddmanager.user.dto.RegisterRequest;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.repository.RoleRepository;
import it.univ.ddmanager.user.repository.UserRepository;

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
class CampaignPlayerIntegrationTest {

    private static final String GM_EMAIL = "gm_campaign_player@example.com";
    private static final String GM_PASSWORD = "CampaignPlayerGm123!";
    private static final String PLAYER_EMAIL = "player_campaign_player@example.com";
    private static final String PLAYER_PASSWORD = "CampaignPlayer123!";
    private static final String SECOND_PLAYER_EMAIL = "player_two_campaign@example.com";
    private static final String SECOND_PLAYER_PASSWORD = "PlayerTwo123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestDatabaseCleanup databaseCleanup;

    @Autowired
    private TestAuthenticationHelper authHelper;

    @BeforeEach
    void cleanDatabase() {
        databaseCleanup.truncateAll();
    }

    @Test
    void player_requestFlowAndGmApproval() throws Exception {
        setupGm();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "CampaignPlayer", "PLAYER");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Campaign Player World");
        Long campaignId = createCampaign(GM_EMAIL, GM_PASSWORD, worldId, "Join Campaign");
        Long characterId = createCharacter(PLAYER_EMAIL, PLAYER_PASSWORD, worldId, "Joiner", true);

        Long campaignPlayerId = requestJoin(campaignId, characterId, PLAYER_EMAIL, PLAYER_PASSWORD);

        mockMvc.perform(get("/api/campaigns/{campaignId}/my-join-request", campaignId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.characterId").value(characterId))
                .andExpect(jsonPath("$.message").value("Vorrei partecipare"))
                .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(get("/api/campaigns/{campaignId}/my-join-request", campaignId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/campaigns/{campaignId}/join-requests", campaignId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(joinPayload(characterId)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/campaigns/{campaignId}/join-requests", campaignId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].message").value("Vorrei partecipare"))
                .andExpect(jsonPath("$[0].characterSubclass").value("Hunter"));

        mockMvc.perform(get("/api/dm/join-requests").with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].campaignId").value(campaignId));

        mockMvc.perform(post("/api/campaigns/{campaignId}/join-requests/{campaignPlayerId}/approve",
                campaignId, campaignPlayerId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decisionByNickname").value("CampaignGM"));

        mockMvc.perform(get("/api/campaigns/{campaignId}/my-join-request", campaignId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decisionByNickname").value("CampaignGM"));

        mockMvc.perform(get("/api/campaigns/{campaignId}/players", campaignId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[0].characterId").value(characterId))
                .andExpect(jsonPath("$[0].characterSubclass").value("Hunter"))
                .andExpect(jsonPath("$[0].decisionByNickname").value("CampaignGM"));

        mockMvc.perform(get("/api/campaign-players/my").with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[0].characterSubclass").value("Hunter"))
                .andExpect(jsonPath("$[0].message").value("Vorrei partecipare"));
    }

    @Test
    void nonOwnerOrAdminCannotManageJoinRequests() throws Exception {
        setupGm();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "CampaignPlayer", "PLAYER");
        registerUser(SECOND_PLAYER_EMAIL, SECOND_PLAYER_PASSWORD, "Intruder", "PLAYER");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Security World");
        Long campaignId = createCampaign(GM_EMAIL, GM_PASSWORD, worldId, "Sec Campaign");
        Long characterId = createCharacter(PLAYER_EMAIL, PLAYER_PASSWORD, worldId, "Hero", true);
        Long campaignPlayerId = requestJoin(campaignId, characterId, PLAYER_EMAIL, PLAYER_PASSWORD);

        mockMvc.perform(get("/api/campaigns/{campaignId}/my-join-request", campaignId)
                .with(authHelper.bearerToken(SECOND_PLAYER_EMAIL, SECOND_PLAYER_PASSWORD)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/campaigns/{campaignId}/join-requests", campaignId)
                .with(authHelper.bearerToken(SECOND_PLAYER_EMAIL, SECOND_PLAYER_PASSWORD)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/campaigns/{campaignId}/join-requests/{campaignPlayerId}/approve",
                campaignId, campaignPlayerId)
                .with(authHelper.bearerToken(SECOND_PLAYER_EMAIL, SECOND_PLAYER_PASSWORD)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/dm/join-requests")
                .with(authHelper.bearerToken(SECOND_PLAYER_EMAIL, SECOND_PLAYER_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void worldMismatchAndVisibilityRules() throws Exception {
        setupGm();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "CampaignPlayer", "PLAYER");
        registerUser(SECOND_PLAYER_EMAIL, SECOND_PLAYER_PASSWORD, "SidePlayer", "PLAYER");
        Long worldOne = createWorld(GM_EMAIL, GM_PASSWORD, "World One");
        Long worldTwo = createWorld(GM_EMAIL, GM_PASSWORD, "World Two");
        Long campaignId = createCampaign(GM_EMAIL, GM_PASSWORD, worldTwo, "Visibility Campaign");

        Long mismatchCharacterId = createCharacter(PLAYER_EMAIL, PLAYER_PASSWORD, worldOne, "Mismatch", true);
        mockMvc.perform(post("/api/campaigns/{campaignId}/join-requests", campaignId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(joinPayload(mismatchCharacterId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.characterId").value(mismatchCharacterId));

        Long hiddenCharacterId = createCharacter(PLAYER_EMAIL, PLAYER_PASSWORD, worldTwo, "Hidden Champ", false);
        Long campaignPlayerId = requestJoin(campaignId, hiddenCharacterId, PLAYER_EMAIL, PLAYER_PASSWORD);
        mockMvc.perform(post("/api/campaigns/{campaignId}/join-requests/{campaignPlayerId}/approve",
                campaignId, campaignPlayerId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/campaigns/{campaignId}/players", campaignId)
                .with(authHelper.bearerToken(SECOND_PLAYER_EMAIL, SECOND_PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/campaigns/{campaignId}/players", campaignId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].characterId").value(hiddenCharacterId));
    }

    @Test
    void gm_canUseAggregatedJoinRequestEndpoint() throws Exception {
        setupGm();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "PlayerOne", "PLAYER");
        registerUser(SECOND_PLAYER_EMAIL, SECOND_PLAYER_PASSWORD, "PlayerTwo", "PLAYER");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Dashboard World");
        Long campaignOne = createCampaign(GM_EMAIL, GM_PASSWORD, worldId, "First Campaign");
        Long campaignTwo = createCampaign(GM_EMAIL, GM_PASSWORD, worldId, "Second Campaign");
        Long firstCharacter = createCharacter(PLAYER_EMAIL, PLAYER_PASSWORD, worldId, "First Hero", true);
        Long secondCharacter = createCharacter(SECOND_PLAYER_EMAIL, SECOND_PLAYER_PASSWORD, worldId, "Second Hero",
                true);

        Long firstJoin = requestJoin(campaignOne, firstCharacter, PLAYER_EMAIL, PLAYER_PASSWORD);
        requestJoin(campaignTwo, secondCharacter, SECOND_PLAYER_EMAIL, SECOND_PLAYER_PASSWORD);

        mockMvc.perform(get("/api/dm/join-requests").with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].campaignId").isNumber());

        mockMvc.perform(get("/api/dm/join-requests").with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/campaigns/{campaignId}/join-requests/{campaignPlayerId}/approve",
                campaignOne, firstJoin)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dm/join-requests").with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].characterId").value(secondCharacter))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void joinRequestsMy_returnsDataForPlayersAndHandlesPermissions() throws Exception {
        setupGm();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "ViewerPlayer", "PLAYER");

        mockMvc.perform(get("/api/join-requests/my").with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Public World");
        Long campaignId = createCampaign(GM_EMAIL, GM_PASSWORD, worldId, "Open Campaign");
        Long characterId = createCharacter(PLAYER_EMAIL, PLAYER_PASSWORD, worldId, "Sheet Hero", true);
        requestJoin(campaignId, characterId, PLAYER_EMAIL, PLAYER_PASSWORD);

        mockMvc.perform(get("/api/join-requests/my").with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].campaignId").value(campaignId))
                .andExpect(jsonPath("$[0].characterId").value(characterId))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mockMvc.perform(get("/api/join-requests/my"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/join-requests/my").with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    private void setupGm() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "CampaignGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
    }

    private void registerUser(String email, String password, String nickname, String role) throws Exception {
        RegisterRequest request = new RegisterRequest(email, password, nickname, role);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private void grantRole(String email, String roleName) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));
        user.getRoles().add(role);
        userRepository.save(user);
    }

    private Long createWorld(String email, String password, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/worlds")
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(worldPayload(name)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Long createCampaign(String email, String password, Long worldId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns")
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(campaignPayload(worldId, name)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Long createCharacter(String email, String password, Long worldId, String name, boolean visible)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/characters")
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(characterPayload(worldId, name, visible)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Long requestJoin(Long campaignId, Long characterId, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns/{campaignId}/join-requests", campaignId)
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(joinPayload(characterId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Vorrei partecipare"))
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private String worldPayload(String name) {
        return String.format("{\"name\":\"%s\",\"description\":\"desc\"}", name);
    }

    private String campaignPayload(Long worldId, String name) {
        return String.format("{\"worldId\":%d,\"name\":\"%s\",\"description\":\"desc\",\"status\":\"ACTIVE\"}", worldId,
                name);
    }

    private String characterPayload(Long worldId, String name, boolean visible) {
        return String.format(
                "{\"worldId\":%d,\"name\":\"%s\",\"race\":\"Elf\",\"characterClass\":\"Ranger\",\"subclass\":\"Hunter\",\"level\":2,"
                        + "\"inspiration\":false,\"strength\":14,\"dexterity\":15,\"constitution\":13,"
                        + "\"strengthSaveProficient\":true,\"acrobaticsProficient\":false,"
                        + "\"maxHitPoints\":18,\"currentHitPoints\":18,\"armorClass\":14,\"speed\":30,"
                        + "\"spellcastingClass\":\"Ranger\",\"spellSlots\":\"1:2\",\"gmNotes\":\"notes\",\"isVisibleToPlayers\":%s}",
                worldId, name, visible);
    }

    private String joinPayload(Long characterId) {
        return String.format("{\"characterId\":%d,\"message\":\"Vorrei partecipare\"}", characterId);
    }
}
