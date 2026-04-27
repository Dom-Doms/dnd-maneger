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
class CampaignCrudIntegrationTest {

    private static final String GM_EMAIL = "gm_campaign_crud@example.com";
    private static final String GM_PASSWORD = "CrudGmPass123!";
    private static final String PLAYER_EMAIL = "player_campaign_crud@example.com";
    private static final String PLAYER_PASSWORD = "CrudPlayerPass123!";

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
    void gm_canUpdateAndDeleteOwnCampaign() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "CrudGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Crud World");
        Long campaignId = createCampaign(GM_EMAIL, GM_PASSWORD, worldId, "Old Name");

        mockMvc.perform(put("/api/campaigns/{id}", campaignId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(campaignPayload(worldId, "New Name", "Desc", "ACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));

        mockMvc.perform(delete("/api/campaigns/{id}", campaignId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/campaigns/{id}", campaignId).with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void player_cannotUpdateOrDeleteCampaign() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "CrudGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Player World");
        Long campaignId = createCampaign(GM_EMAIL, GM_PASSWORD, worldId, "PlayerBlocked");

        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "CrudPlayer", "PLAYER");

        mockMvc.perform(put("/api/campaigns/{id}", campaignId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(campaignPayload(worldId, "Blocked Update", "Desc", "ACTIVE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/campaigns/{id}", campaignId)
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

    private Long createWorld(String email, String password, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/worlds")
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(worldPayload(name, "world desc")))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Long createCampaign(String email, String password, Long worldId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns")
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(campaignPayload(worldId, name, "desc", "ACTIVE")))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private String worldPayload(String name, String description) {
        return String.format("{\"name\":\"%s\",\"description\":\"%s\"}", name, description);
    }

    private String campaignPayload(Long worldId, String name, String description, String status) {
        return String.format("{\"worldId\":%d,\"name\":\"%s\",\"description\":\"%s\",\"status\":\"%s\"}",
                worldId, name, description, status);
    }
}
