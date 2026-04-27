package it.univ.ddmanager.item;

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
class ItemIntegrationTest {

    private static final String GM_EMAIL = "gm_item@example.com";
    private static final String GM_PASSWORD = "ItemGmPass123!";
    private static final String PLAYER_EMAIL = "player_item@example.com";
    private static final String PLAYER_PASSWORD = "ItemPlayerPass123!";

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
    void gm_canCreateAndListItems() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "ItemGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Item World");

        mockMvc.perform(post("/api/items")
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemPayload(worldId, null, "Sword of Dawn", "weapon", "rare", true, "gm secret")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sword of Dawn"))
                .andExpect(jsonPath("$.gmNotes").value("gm secret"));

        mockMvc.perform(get("/api/items").with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sword of Dawn"));
    }

    @Test
    void player_seesOnlyVisibleItems_andNoGmNotes() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "ItemGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Visibility World");

        Long visibleId = createItem(GM_EMAIL, GM_PASSWORD, worldId, null, "Healing Potion", "potion", "common", true,
                "gm only");
        Long hiddenId = createItem(GM_EMAIL, GM_PASSWORD, worldId, null, "Cursed Dagger", "weapon", "rare", false,
                "top secret");

        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "ItemPlayer", "PLAYER");

        mockMvc.perform(get("/api/items").with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Healing Potion"))
                .andExpect(jsonPath("$[0].gmNotes").value(nullValue()));

        mockMvc.perform(get("/api/items/{id}", visibleId).with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Healing Potion"))
                .andExpect(jsonPath("$.gmNotes").value(nullValue()));

        mockMvc.perform(get("/api/items/{id}", hiddenId).with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void gm_seesAllItems_andGmNotes() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "ItemGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "GM View World");

        createItem(GM_EMAIL, GM_PASSWORD, worldId, null, "Shield", "armor", "common", true, "shield note");
        createItem(GM_EMAIL, GM_PASSWORD, worldId, null, "Cloak", "gear", "legendary", false, "cloak note");

        mockMvc.perform(get("/api/items").with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].gmNotes", containsInAnyOrder("shield note", "cloak note")));
    }

    @Test
    void player_cannotCreateOrDeleteItems() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "ItemGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Delete World");
        Long itemId = createItem(GM_EMAIL, GM_PASSWORD, worldId, null, "Target Item", "gear", "rare", true, "gm note");

        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "ItemPlayer", "PLAYER");

        mockMvc.perform(post("/api/items")
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemPayload(worldId, null, "Player Item", "gear", "common", true, "should fail")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/items/{id}", itemId)
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
                .content(worldPayload(name, "World for Items")))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Long createItem(String email, String password, Long worldId, Long locationId, String name, String type,
            String rarity, boolean visible, String gmNotes) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/items")
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemPayload(worldId, locationId, name, type, rarity, visible, gmNotes)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private String worldPayload(String name, String description) {
        return String.format("{\"name\":\"%s\",\"description\":\"%s\"}", name, description);
    }

    private String itemPayload(Long worldId, Long locationId, String name, String type, String rarity, boolean visible,
            String gmNotes) {
        String location = locationId != null ? locationId.toString() : "null";
        return String.format(
                "{\"worldId\":%d,\"locationId\":%s,\"name\":\"%s\",\"type\":\"%s\",\"rarity\":\"%s\",\"description\":\"desc\",\"gmNotes\":\"%s\",\"isVisibleToPlayers\":%s}",
                worldId, location, name, type, rarity, gmNotes, visible);
    }
}
