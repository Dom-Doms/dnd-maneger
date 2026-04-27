package it.univ.ddmanager.sessionlog;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.univ.ddmanager.campaign.repository.CampaignRepository;
import it.univ.ddmanager.campaign.repository.SessionRepository;
import it.univ.ddmanager.sessionlog.repository.SessionEventRepository;
import it.univ.ddmanager.support.TestAuthenticationHelper;
import it.univ.ddmanager.support.TestDatabaseCleanup;
import it.univ.ddmanager.user.dto.RegisterRequest;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.repository.RoleRepository;
import it.univ.ddmanager.user.repository.UserRepository;
import it.univ.ddmanager.world.repository.WorldRepository;
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
class SessionEventIntegrationTest {

    private static final String GM_EMAIL = "gm_session_event@example.com";
    private static final String GM_PASSWORD = "SessionEventGm123!";
    private static final String PLAYER_EMAIL = "player_session_event@example.com";
    private static final String PLAYER_PASSWORD = "SessionEventPlayer123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SessionEventRepository sessionEventRepository;

    @Autowired
    private TestDatabaseCleanup databaseCleanup;

    @Autowired
    private TestAuthenticationHelper authHelper;

    @BeforeEach
    void cleanDatabase() {
        databaseCleanup.truncateAll();
    }

    @Test
    void gm_canCreateAndListSessionEvents() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "SessionGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long sessionId = createSessionHierarchy(GM_EMAIL, GM_PASSWORD);

        mockMvc.perform(post("/api/session-events")
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventPayload(sessionId, "Battle Start", true)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Battle Start"));

        mockMvc.perform(get("/api/sessions/{sessionId}/events", sessionId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void player_seesOnlyVisibleSessionEvents() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "SessionGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long sessionId = createSessionHierarchy(GM_EMAIL, GM_PASSWORD);

        createEvent(GM_EMAIL, GM_PASSWORD, sessionId, "Visible Event", true);
        createEvent(GM_EMAIL, GM_PASSWORD, sessionId, "Hidden Event", false);

        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "SessionPlayer", "PLAYER");

        mockMvc.perform(get("/api/sessions/{sessionId}/events", sessionId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].title").value("Visible Event"));
    }

    @Test
    void gm_seesAllSessionEvents() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "SessionGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long sessionId = createSessionHierarchy(GM_EMAIL, GM_PASSWORD);

        createEvent(GM_EMAIL, GM_PASSWORD, sessionId, "Visible Event", true);
        createEvent(GM_EMAIL, GM_PASSWORD, sessionId, "Hidden Event", false);

        mockMvc.perform(get("/api/sessions/{sessionId}/events", sessionId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void player_cannotCreateOrDeleteSessionEvents() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "SessionGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long sessionId = createSessionHierarchy(GM_EMAIL, GM_PASSWORD);
        Long eventId = createEvent(GM_EMAIL, GM_PASSWORD, sessionId, "GM Event", true);

        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "SessionPlayer", "PLAYER");

        mockMvc.perform(post("/api/session-events")
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventPayload(sessionId, "Player Event", true)))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/session-events/{id}", eventId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
            .andExpect(status().isForbidden());
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

    private Long createSessionHierarchy(String email, String password) throws Exception {
        Long worldId = createWorld(email, password, "SessionLog World");
        Long campaignId = createCampaign(email, password, worldId, "SessionLog Campaign");
        return createSession(email, password, campaignId, "SessionLog Session");
    }

    private Long createWorld(String email, String password, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/worlds")
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"name\":\"%s\",\"description\":\"desc\"}", name)))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Long createCampaign(String email, String password, Long worldId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns")
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"worldId\":%d,\"name\":\"%s\",\"description\":\"desc\",\"status\":\"ACTIVE\"}", worldId, name)))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Long createSession(String email, String password, Long campaignId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns/{campaignId}/sessions", campaignId)
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"title\":\"%s\",\"sessionNumber\":1,\"sessionDate\":\"2024-01-01\",\"notes\":\"notes\"}", title)))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Long createEvent(String email, String password, Long sessionId, String title, boolean visible) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/session-events")
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventPayload(sessionId, title, visible)))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private String eventPayload(Long sessionId, String title, boolean visible) {
        return String.format(
            "{\"sessionId\":%d,\"title\":\"%s\",\"type\":\"NOTE\",\"description\":\"desc\",\"inGameTime\":\"Day 1\",\"isVisibleToPlayers\":%s}",
            sessionId, title, visible);
    }
}
