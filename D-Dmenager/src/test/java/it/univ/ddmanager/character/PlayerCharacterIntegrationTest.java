package it.univ.ddmanager.character;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
class PlayerCharacterIntegrationTest {

    private static final String GM_EMAIL = "gm_character@example.com";
    private static final String GM_PASSWORD = "CharacterGmPass123!";
    private static final String PLAYER_EMAIL = "player_character@example.com";
    private static final String PLAYER_PASSWORD = "CharacterPlayerPass123!";
    private static final String OTHER_PLAYER_EMAIL = "other_player_character@example.com";
    private static final String OTHER_PLAYER_PASSWORD = "OtherCharacter123!";

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
    void player_characterCreationRequiresMandatoryFields() throws Exception {
        setupGmAndWorld();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "CharacterPlayer", "PLAYER");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Validation World");

        ObjectNode missingName = characterPayloadNode(worldId, "Missing Name", true, "gm");
        missingName.put("name", "");

        mockMvc.perform(post("/api/characters")
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingName)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("name")));

        ObjectNode missingLevel = characterPayloadNode(worldId, "Missing Level", true, "gm");
        missingLevel.remove("level");

        mockMvc.perform(post("/api/characters")
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingLevel)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("level")));

        ObjectNode missingMaxHp = characterPayloadNode(worldId, "Missing HP", true, "gm");
        missingMaxHp.remove("maxHitPoints");

        mockMvc.perform(post("/api/characters")
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingMaxHp)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("maxHitPoints")));

        ObjectNode missingCurrentHp = characterPayloadNode(worldId, "Missing Current HP", true, "gm");
        missingCurrentHp.remove("currentHitPoints");

        mockMvc.perform(post("/api/characters")
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingCurrentHp)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("currentHitPoints")));
    }

    @Test
    void player_characterCreationRejectsAbilityScoresOutOfRange() throws Exception {
        setupGmAndWorld();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "CharacterPlayer", "PLAYER");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Ability World");

        ObjectNode payload = characterPayloadNode(worldId, "Overpowered", true, "gm");
        payload.put("strength", 40);

        mockMvc.perform(post("/api/characters")
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("strength")));
    }

    @Test
    void player_cannotSetWorldDuringCreateOrUpdate() throws Exception {
        setupGmAndWorld();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "CharacterPlayer", "PLAYER");
        Long worldOne = createWorld(GM_EMAIL, GM_PASSWORD, "World One");
        Long worldTwo = createWorld(GM_EMAIL, GM_PASSWORD, "World Two");

        ObjectNode payload = characterPayloadNode(worldOne, "Traveler", true, "gm secret");
        MvcResult result = mockMvc.perform(post("/api/characters")
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.worldId").value(nullValue()))
            .andReturn();
        Long characterId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        ObjectNode updatePayload = characterPayloadNode(worldTwo, "Traveler Updated", true, "gm secret");
        mockMvc.perform(put("/api/characters/{id}", characterId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatePayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.worldId").value(nullValue()))
            .andExpect(jsonPath("$.name").value("Traveler Updated"));
    }

    @Test
    void gm_canAssignWorldDuringUpdate() throws Exception {
        setupGmAndWorld();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "CharacterPlayer", "PLAYER");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Managed World");
        Long characterId = createCharacter(PLAYER_EMAIL, PLAYER_PASSWORD, worldId, "Assignable", true, "gm only");

        ObjectNode payload = characterPayloadNode(worldId, "Assignable", true, "gm only");
        mockMvc.perform(put("/api/characters/{id}", characterId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.worldId").value(worldId));
    }

    @Test
    void player_canCreateCharacterAndListOwn() throws Exception {
        setupGmAndWorld();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "CharacterPlayer", "PLAYER");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Character World");

        mockMvc.perform(post("/api/characters")
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(characterPayload(worldId, "Aelar", true, "owner gm notes")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.worldId").value(nullValue()))
            .andExpect(jsonPath("$.name").value("Aelar"))
            .andExpect(jsonPath("$.subclass").value("Evoker"))
            .andExpect(jsonPath("$.inspiration").value(true))
            .andExpect(jsonPath("$.strengthSaveProficient").value(true))
            .andExpect(jsonPath("$.acrobaticsProficient").value(true))
            .andExpect(jsonPath("$.temporaryHitPoints").value(5))
            .andExpect(jsonPath("$.passivePerception").value(14))
            .andExpect(jsonPath("$.spellcastingClass").value("Wizard"))
            .andExpect(jsonPath("$.spellSlots").value("1:4,2:2"))
            .andExpect(jsonPath("$.gmNotes").value("owner gm notes"));

        mockMvc.perform(get("/api/characters/my").with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("Aelar"))
            .andExpect(jsonPath("$[0].worldId").value(nullValue()))
            .andExpect(jsonPath("$[0].passiveInsight").value(13))
            .andExpect(jsonPath("$[0].gmNotes").value("owner gm notes"));
    }

    @Test
    void player_cannotModifyOrDeleteOthersCharacter() throws Exception {
        setupGmAndWorld();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "CharacterPlayer", "PLAYER");
        registerUser(OTHER_PLAYER_EMAIL, OTHER_PLAYER_PASSWORD, "OtherPlayer", "PLAYER");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Ownership World");
        Long characterId = createCharacter(PLAYER_EMAIL, PLAYER_PASSWORD, worldId, "Owner Character", true, "private");

        mockMvc.perform(put("/api/characters/{id}", characterId)
                .with(authHelper.bearerToken(OTHER_PLAYER_EMAIL, OTHER_PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(characterPayload(worldId, "Hacker", true, "attempt")))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/characters/{id}", characterId)
                .with(authHelper.bearerToken(OTHER_PLAYER_EMAIL, OTHER_PLAYER_PASSWORD)))
            .andExpect(status().isForbidden());
    }

    @Test
    void visibility_rulesAppliedBetweenPlayers() throws Exception {
        setupGmAndWorld();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "CharacterPlayer", "PLAYER");
        registerUser(OTHER_PLAYER_EMAIL, OTHER_PLAYER_PASSWORD, "OtherPlayer", "PLAYER");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Visibility World");

        Long visibleId = createCharacter(PLAYER_EMAIL, PLAYER_PASSWORD, worldId, "Visible Hero", true, "visible");
        Long hiddenId = createCharacter(PLAYER_EMAIL, PLAYER_PASSWORD, worldId, "Hidden Hero", false, "secret");

        mockMvc.perform(get("/api/characters/{id}", visibleId)
                .with(authHelper.bearerToken(OTHER_PLAYER_EMAIL, OTHER_PLAYER_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gmNotes").value(nullValue()));

        mockMvc.perform(get("/api/characters/{id}", hiddenId)
                .with(authHelper.bearerToken(OTHER_PLAYER_EMAIL, OTHER_PLAYER_PASSWORD)))
            .andExpect(status().isNotFound());
    }

    @Test
    void gm_seesAllCharactersAndGmNotes() throws Exception {
        setupGmAndWorld();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "CharacterPlayer", "PLAYER");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "GM View World");
        Long visibleId = createCharacter(PLAYER_EMAIL, PLAYER_PASSWORD, worldId, "Visible Hero", true, "gm note");
        Long hiddenId = createCharacter(PLAYER_EMAIL, PLAYER_PASSWORD, worldId, "Hidden Rogue", false, "secret gm note");

        mockMvc.perform(get("/api/characters/{id}", visibleId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gmNotes").value("gm note"));

        mockMvc.perform(get("/api/characters/{id}", hiddenId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gmNotes").value("secret gm note"));
    }

    @Test
    void owner_canUseCharacterSheetPatchEndpointsAndLongRest() throws Exception {
        setupGmAndWorld();
        registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "CharacterPlayer", "PLAYER");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Character Sheet World");
        Long characterId = createCharacter(PLAYER_EMAIL, PLAYER_PASSWORD, worldId, "Sheet Hero", true, "gm only");

        mockMvc.perform(patch("/api/characters/{id}/hp", characterId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentHitPoints\":12}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentHitPoints").value(12));

        mockMvc.perform(patch("/api/characters/{id}/death-saves", characterId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"successes\":1,\"failures\":3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deathSaveSuccesses").value(1))
            .andExpect(jsonPath("$.deathSaveFailures").value(3));

        mockMvc.perform(patch("/api/characters/{id}/inventory", characterId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipment\":\"Rope, Torch\",\"treasure\":\"120gp\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.equipment").value("Rope, Torch"))
            .andExpect(jsonPath("$.treasure").value("120gp"));

        mockMvc.perform(patch("/api/characters/{id}/spell-slots", characterId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"spellSlots\":\"1:1/4,2:0/2\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.spellSlots").value("1:1/4,2:0/2"));

        mockMvc.perform(post("/api/characters/{id}/long-rest", characterId)
                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentHitPoints").value(27))
            .andExpect(jsonPath("$.deathSaveSuccesses").value(0))
            .andExpect(jsonPath("$.deathSaveFailures").value(0))
            .andExpect(jsonPath("$.spellSlots").value("1:4/4,2:2/2"));
    }

    private void setupGmAndWorld() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "CharacterGM", "DM");
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

    private Long createCharacter(String email, String password, Long worldId, String name, boolean visible, String gmNotes)
        throws Exception {
        MvcResult result = mockMvc.perform(post("/api/characters")
                .with(authHelper.bearerToken(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(characterPayload(worldId, name, visible, gmNotes)))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private String worldPayload(String name) {
        return String.format("{\"name\":\"%s\",\"description\":\"desc\"}", name);
    }

    private ObjectNode characterPayloadNode(Long worldId, String name, boolean visible, String gmNotes) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("worldId", worldId);
        node.put("name", name);
        node.put("race", "Elf");
        node.put("characterClass", "Wizard");
        node.put("subclass", "Evoker");
        node.put("background", "Sage");
        node.put("alignment", "Neutral Good");
        node.put("level", 3);
        node.put("experiencePoints", 900);
        node.put("inspiration", true);
        node.put("proficiencyBonus", 2);
        node.put("strength", 10);
        node.put("dexterity", 16);
        node.put("constitution", 14);
        node.put("intelligence", 17);
        node.put("wisdom", 12);
        node.put("charisma", 11);
        node.put("strengthSaveProficient", true);
        node.put("dexteritySaveProficient", false);
        node.put("constitutionSaveProficient", true);
        node.put("intelligenceSaveProficient", true);
        node.put("wisdomSaveProficient", false);
        node.put("charismaSaveProficient", false);
        node.put("acrobaticsProficient", true);
        node.put("animalHandlingProficient", false);
        node.put("arcanaProficient", true);
        node.put("athleticsProficient", false);
        node.put("deceptionProficient", false);
        node.put("historyProficient", true);
        node.put("insightProficient", true);
        node.put("intimidationProficient", false);
        node.put("investigationProficient", true);
        node.put("medicineProficient", false);
        node.put("natureProficient", false);
        node.put("perceptionProficient", true);
        node.put("performanceProficient", false);
        node.put("persuasionProficient", false);
        node.put("religionProficient", true);
        node.put("sleightOfHandProficient", false);
        node.put("stealthProficient", false);
        node.put("survivalProficient", false);
        node.put("maxHitPoints", 27);
        node.put("currentHitPoints", 27);
        node.put("temporaryHitPoints", 5);
        node.put("armorClass", 15);
        node.put("speed", 30);
        node.put("initiativeModifier", 2);
        node.put("hitDice", "3d6");
        node.put("deathSaveSuccesses", 1);
        node.put("deathSaveFailures", 0);
        node.put("passivePerception", 14);
        node.put("passiveInvestigation", 12);
        node.put("passiveInsight", 13);
        node.put("personalityTraits", "Curious");
        node.put("ideals", "Knowledge");
        node.put("bonds", "University");
        node.put("flaws", "Arrogant");
        node.put("appearance", "Tall elf");
        node.put("backstory", "Spent years studying magic.");
        node.put("featuresAndTraits", "Arcane Recovery");
        node.put("alliesAndOrganizations", "Harpers");
        node.put("treasure", "50gp");
        node.put("otherProficiencies", "Thieves tools");
        node.put("proficienciesAndLanguages", "Common, Elvish");
        node.put("attacksAndSpellcasting", "Quarterstaff +4");
        node.put("equipment", "Spellbook");
        node.put("spells", "Mage Hand");
        node.put("spellcastingClass", "Wizard");
        node.put("spellSaveDC", 14);
        node.put("spellAttackBonus", 6);
        node.put("knownSpells", "Mage Hand, Shield");
        node.put("preparedSpells", "Magic Missile");
        node.put("spellSlots", "1:4,2:2");
        node.put("otherNotes", "Notes");
        node.put("gmNotes", gmNotes);
        node.put("isVisibleToPlayers", visible);
        return node;
    }

    private String characterPayload(Long worldId, String name, boolean visible, String gmNotes) throws Exception {
        return objectMapper.writeValueAsString(characterPayloadNode(worldId, name, visible, gmNotes));
    }
}
