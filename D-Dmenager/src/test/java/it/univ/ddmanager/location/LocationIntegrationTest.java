package it.univ.ddmanager.location;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class LocationIntegrationTest {

    private static final String GM_EMAIL = "gm_location@example.com";
    private static final String GM_PASSWORD = "LocGmPass123!";
    private static final String PLAYER_EMAIL = "player_location@example.com";
    private static final String PLAYER_PASSWORD = "LocPlayerPass123!";

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
    void gm_canCreateAndListLocations() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "LocGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Location World");

        mockMvc.perform(post("/api/locations")
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(locationPayload(worldId, null, "Inn", true, "gm note")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Inn"))
                .andExpect(jsonPath("$.gmNotes").value("gm note"));

        mockMvc.perform(get("/api/locations").with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Inn"));
    }

    @Test
    void player_seesOnlyVisibleLocations_andNoGmNotes() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "LocGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Visibility World");

        Long visibleId = createLocation(GM_EMAIL, GM_PASSWORD, worldId, null, "Town", true, "gm only");
        Long hiddenId = createLocation(GM_EMAIL, GM_PASSWORD, worldId, null, "Secret Base", false, "hidden gm notes");

        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "LocPlayer", "PLAYER");

        mockMvc.perform(get("/api/locations").with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Town"))
                .andExpect(jsonPath("$[0].gmNotes").value(nullValue()));

        mockMvc.perform(get("/api/locations/{id}", visibleId).with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Town"))
                .andExpect(jsonPath("$.gmNotes").value(nullValue()));

        mockMvc.perform(get("/api/locations/{id}", hiddenId).with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void gm_seesAllLocations_andGmNotes() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "LocGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "GM View World");

        createLocation(GM_EMAIL, GM_PASSWORD, worldId, null, "Castle", true, "castle note");
        createLocation(GM_EMAIL, GM_PASSWORD, worldId, null, "Hideout", false, "hideout note");

        mockMvc.perform(get("/api/locations").with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].gmNotes", containsInAnyOrder("castle note", "hideout note")));
    }

    @Test
    void player_cannotCreateOrDeleteLocation() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "LocGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Delete World");
        Long locationId = createLocation(GM_EMAIL, GM_PASSWORD, worldId, null, "Target", true, "gm note");

        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "LocPlayer", "PLAYER");

        mockMvc.perform(post("/api/locations")
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(locationPayload(worldId, null, "Player Loc", true, "should fail")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/locations/{id}", locationId)
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
                .content(worldPayload(name, "World for Locations")))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Long createLocation(String email, String password, Long worldId, Long parentId, String name,
            boolean visible, String gmNotes) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/locations")
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(locationPayload(worldId, parentId, name, visible, gmNotes)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private String worldPayload(String name, String description) {
        return String.format("{\"name\":\"%s\",\"description\":\"%s\"}", name, description);
    }

    private String locationPayload(Long worldId, Long parentId, String name, boolean visible, String gmNotes) {
        String parent = parentId != null ? parentId.toString() : "null";
        return String.format(
                "{\"worldId\":%d,\"parentLocationId\":%s,\"name\":\"%s\",\"type\":null,\"description\":\"desc\",\"gmNotes\":\"%s\",\"isVisibleToPlayers\":%s}",
                worldId, parent, name, gmNotes, visible);
    }
}
