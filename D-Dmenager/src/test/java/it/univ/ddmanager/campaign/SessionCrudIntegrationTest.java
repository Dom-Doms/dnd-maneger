package it.univ.ddmanager.campaign;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class SessionCrudIntegrationTest {

    private static final String GM_EMAIL = "gm_session_crud@example.com";
    private static final String GM_PASSWORD = "SessionCrudGm123!";
    private static final String OTHER_GM_EMAIL = "other_gm_session_crud@example.com";
    private static final String OTHER_GM_PASSWORD = "OtherSessionCrud123!";

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
    void gm_canGetUpdateAndDeleteOwnSession() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "SessionCrudGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "SessionCrud World");
        Long campaignId = createCampaign(GM_EMAIL, GM_PASSWORD, worldId, "SessionCrud Campaign");
        Long sessionId = createSession(GM_EMAIL, GM_PASSWORD, campaignId, "Session 1");

        mockMvc.perform(get("/api/sessions/{sessionId}", sessionId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Session 1"))
            .andExpect(jsonPath("$.campaignId").value(campaignId));

        mockMvc.perform(put("/api/sessions/{sessionId}", sessionId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionPayload("Updated Session", 2, "2024-02-02", "Updated notes")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated Session"))
            .andExpect(jsonPath("$.sessionNumber").value(2))
            .andExpect(jsonPath("$.notes").value("Updated notes"));

        mockMvc.perform(delete("/api/sessions/{sessionId}", sessionId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/sessions/{sessionId}", sessionId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
            .andExpect(status().isNotFound());
    }

    @Test
    void gm_notOwnerCannotUpdateOrDeleteSession() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "SessionCrudGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Owner World");
        Long campaignId = createCampaign(GM_EMAIL, GM_PASSWORD, worldId, "Owner Campaign");
        Long sessionId = createSession(GM_EMAIL, GM_PASSWORD, campaignId, "Session 2");

        registerUser(OTHER_GM_EMAIL, OTHER_GM_PASSWORD, "OtherGM", "DM");
        grantRole(OTHER_GM_EMAIL, "ROLE_GM");

        mockMvc.perform(put("/api/sessions/{sessionId}", sessionId)
                .with(authHelper.bearerToken(OTHER_GM_EMAIL, OTHER_GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionPayload("Hack Session", 3, "2024-03-03", "Should fail")))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/sessions/{sessionId}", sessionId)
                .with(authHelper.bearerToken(OTHER_GM_EMAIL, OTHER_GM_PASSWORD)))
            .andExpect(status().isForbidden());
    }

    @Test
    void getSession_returns404ForMissingSession() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "SessionCrudGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");

        mockMvc.perform(get("/api/sessions/{sessionId}", 9999L)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
            .andExpect(status().isNotFound());
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

    private Long createSession(String email, String password, Long campaignId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns/{campaignId}/sessions", campaignId)
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionPayload(title, 1, "2024-01-01", "notes")))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private String worldPayload(String name) {
        return String.format("{\"name\":\"%s\",\"description\":\"desc\"}", name);
    }

    private String campaignPayload(Long worldId, String name) {
        return String.format("{\"worldId\":%d,\"name\":\"%s\",\"description\":\"desc\",\"status\":\"ACTIVE\"}",
            worldId, name);
    }

    private String sessionPayload(String title, int sessionNumber, String date, String notes) {
        return String.format(
            "{\"title\":\"%s\",\"sessionNumber\":%d,\"sessionDate\":\"%s\",\"notes\":\"%s\"}",
            title, sessionNumber, date, notes);
    }
}
