package it.univ.ddmanager.npc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.univ.ddmanager.npc.model.Npc;
import it.univ.ddmanager.npc.repository.NpcRepository;
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
class NpcIntegrationTest {

    private static final String GM_EMAIL = "gm_npc@example.com";
    private static final String GM_PASSWORD = "NpcGmPass123!";
    private static final String PLAYER_EMAIL = "player_npc@example.com";
    private static final String PLAYER_PASSWORD = "NpcPlayerPass123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private NpcRepository npcRepository;

    @Autowired
    private TestDatabaseCleanup databaseCleanup;

    @Autowired
    private TestAuthenticationHelper authHelper;

    @BeforeEach
    void cleanDatabase() {
        databaseCleanup.truncateAll();
    }

    @Test
    void gm_canCreateAndListNpcs() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "NpcGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "NPC World");

        mockMvc.perform(post("/api/npcs")
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(npcPayload(worldId, "Goblin", true, "secret gm notes")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Goblin"))
                .andExpect(jsonPath("$.gmNotes").value("secret gm notes"));

        mockMvc.perform(get("/api/npcs").with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Goblin"));
    }

    @Test
    void player_seesOnlyVisibleNpcs_andNoGmNotes() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "NpcGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Visibility World");

        Long visibleNpcId = createNpc(GM_EMAIL, GM_PASSWORD, worldId, "Elf Sage", true, "gm only");
        Long hiddenNpcId = createNpc(GM_EMAIL, GM_PASSWORD, worldId, "Hidden Assassin", false, "top secret");

        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "NpcPlayer", "PLAYER");

        mockMvc.perform(get("/api/npcs").with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Elf Sage"))
                .andExpect(jsonPath("$[0].gmNotes").value(nullValue()));

        mockMvc.perform(get("/api/npcs/{id}", visibleNpcId).with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Elf Sage"))
                .andExpect(jsonPath("$.gmNotes").value(nullValue()));

        mockMvc.perform(get("/api/npcs/{id}", hiddenNpcId).with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void gm_seesAllNpcs_andGmNotes() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "NpcGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "GM View World");

        createNpc(GM_EMAIL, GM_PASSWORD, worldId, "Knight", true, "knight note");
        createNpc(GM_EMAIL, GM_PASSWORD, worldId, "Spy", false, "spy note");

        mockMvc.perform(get("/api/npcs").with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].gmNotes", containsInAnyOrder("knight note", "spy note")));
    }

    @Test
    void player_cannotCreateOrDeleteNpc() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "NpcGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Delete World");
        Long npcId = createNpc(GM_EMAIL, GM_PASSWORD, worldId, "Target", true, "gm note");

        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "NpcPlayer", "PLAYER");

        mockMvc.perform(post("/api/npcs")
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(npcPayload(worldId, "Player NPC", true, "should fail")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/npcs/{id}", npcId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void gm_canCreateNpcWithMinimalFields() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "NpcGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Quick World");

        String payload = """
                {
                  "worldId": %d,
                  "name": "Quick Scout"
                }
                """.formatted(worldId);

        MvcResult result = mockMvc.perform(post("/api/npcs")
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Quick Scout"))
                .andExpect(jsonPath("$.alignment").value(nullValue()))
                .andExpect(jsonPath("$.armorClass").value(nullValue()))
                .andExpect(jsonPath("$.traits").value(nullValue()))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Long npcId = response.get("id").asLong();
        Npc saved = npcRepository.findById(npcId)
                .orElseThrow(() -> new IllegalStateException("NPC not found in DB"));
        assertThat(saved.getAlignment()).isNull();
        assertThat(saved.getArmorClass()).isNull();
        assertThat(saved.getTraits()).isNull();
        assertThat(saved.getStrength()).isNull();
        assertThat(saved.isVisibleToPlayers()).isTrue();
    }

    @Test
    void gm_canUpdateNpcWithFullStatblock() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "NpcGM", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Full World");
        Long npcId = createNpc(GM_EMAIL, GM_PASSWORD, worldId, "Base NPC", true, "base note");

        String payload = """
                {
                  "worldId": %d,
                  "name": "Ancient Dragon",
                  "race": "Dragon",
                  "roleOrClass": "Boss",
                  "description": "Elder wyrm guarding treasure",
                  "gmNotes": "Weak to cold iron",
                  "isVisibleToPlayers": false,
                  "alignment": "Chaotic Evil",
                  "size": "Gargantuan",
                  "creatureType": "Dragon",
                  "armorClass": 22,
                  "maxHitPoints": 546,
                  "currentHitPoints": 500,
                  "temporaryHitPoints": 30,
                  "hitDice": "28d20 + 224",
                  "speed": "40 ft., fly 80 ft.",
                  "strength": 30,
                  "dexterity": 10,
                  "constitution": 29,
                  "intelligence": 18,
                  "wisdom": 15,
                  "charisma": 23,
                  "savingThrows": "Dex +7, Con +16, Wis +10, Cha +15",
                  "skills": "Perception +17, Stealth +7",
                  "damageResistances": "cold",
                  "damageImmunities": "fire",
                  "conditionImmunities": "frightened",
                  "senses": "blindsight 60 ft., passive Perception 27",
                  "languages": "Common, Draconic, telepathy 120 ft.",
                  "challengeRating": "24",
                  "experiencePoints": 62000,
                  "difficultyClass": 19,
                  "traits": "Legendary Resistance (3/Day).",
                  "actions": "Multiattack, Fire Breath.",
                  "legendaryActions": "Detect, Tail Attack, Wing Attack.",
                  "reactions": "Parry.",
                  "lairActions": "Volcanic tremors.",
                  "regionalEffects": "Volcanic storms."
                }
                """.formatted(worldId);

        mockMvc.perform(put("/api/npcs/{id}", npcId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ancient Dragon"))
                .andExpect(jsonPath("$.alignment").value("Chaotic Evil"))
                .andExpect(jsonPath("$.armorClass").value(22))
                .andExpect(jsonPath("$.currentHitPoints").value(500))
                .andExpect(jsonPath("$.skills").value("Perception +17, Stealth +7"))
                .andExpect(jsonPath("$.challengeRating").value("24"))
                .andExpect(jsonPath("$.legendaryActions").value("Detect, Tail Attack, Wing Attack."))
                .andExpect(jsonPath("$.isVisibleToPlayers").value(false));

        Npc updated = npcRepository.findById(npcId)
                .orElseThrow(() -> new IllegalStateException("NPC not found in DB"));
        assertThat(updated.getAlignment()).isEqualTo("Chaotic Evil");
        assertThat(updated.getArmorClass()).isEqualTo(22);
        assertThat(updated.getCurrentHitPoints()).isEqualTo(500);
        assertThat(updated.getSavingThrows()).isEqualTo("Dex +7, Con +16, Wis +10, Cha +15");
        assertThat(updated.getTraits()).isEqualTo("Legendary Resistance (3/Day).");
        assertThat(updated.getActions()).isEqualTo("Multiattack, Fire Breath.");
        assertThat(updated.getLegendaryActions()).isEqualTo("Detect, Tail Attack, Wing Attack.");
        assertThat(updated.getLairActions()).isEqualTo("Volcanic tremors.");
        assertThat(updated.getRegionalEffects()).isEqualTo("Volcanic storms.");
        assertThat(updated.isVisibleToPlayers()).isFalse();
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
                .content(worldPayload(name, "World for NPCs")))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Long createNpc(String email, String password, Long worldId, String name, boolean visible, String gmNotes)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/npcs")
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(npcPayload(worldId, name, visible, gmNotes)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private String worldPayload(String name, String description) {
        return String.format("{\"name\":\"%s\",\"description\":\"%s\"}", name, description);
    }

    private String npcPayload(Long worldId, String name, boolean visible, String gmNotes) {
        return String.format(
                "{\"worldId\":%d,\"name\":\"%s\",\"race\":null,\"roleOrClass\":null,\"description\":\"desc\",\"gmNotes\":\"%s\",\"isVisibleToPlayers\":%s}",
                worldId, name, gmNotes, visible);
    }
}
