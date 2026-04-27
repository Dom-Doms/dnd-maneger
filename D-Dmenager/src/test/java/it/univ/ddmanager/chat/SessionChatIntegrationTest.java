package it.univ.ddmanager.chat;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.univ.ddmanager.support.TestAuthenticationHelper;
import it.univ.ddmanager.support.TestDatabaseCleanup;
import it.univ.ddmanager.user.dto.RegisterRequest;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.user.repository.RoleRepository;
import it.univ.ddmanager.user.repository.UserRepository;
import java.util.Objects;
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
class SessionChatIntegrationTest {

        private static final String GM_EMAIL = "gm_chat@example.com";
        private static final String GM_PASSWORD = "ChatGmPass123!";
        private static final String PLAYER_ONE_EMAIL = "player_elvish@example.com";
        private static final String PLAYER_ONE_PASSWORD = "PlayerElvish123!";
        private static final String PLAYER_TWO_EMAIL = "player_common@example.com";
        private static final String PLAYER_TWO_PASSWORD = "PlayerCommon123!";
        private static final String VIEWER_EMAIL = "viewer_chat@example.com";
        private static final String VIEWER_PASSWORD = "ViewerChat123!";

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
        void playerSeesObfuscationWhenLanguageUnknown() throws Exception {
                SessionContext context = setupSessionWithPlayers();

                mockMvc.perform(post("/api/sessions/{sessionId}/chat/messages", context.sessionId())
                                .with(Objects.requireNonNull(authHelper.bearerToken(PLAYER_ONE_EMAIL, PLAYER_ONE_PASSWORD)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(chatPayload(context.playerOneCharacterId(), "ELVISH", "For the Seldarine")))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.contentVisible").value("For the Seldarine"));

                mockMvc.perform(get("/api/sessions/{sessionId}/chat/messages", context.sessionId())
                                .with(Objects.requireNonNull(authHelper.bearerToken(PLAYER_ONE_EMAIL, PLAYER_ONE_PASSWORD))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].contentVisible").value("For the Seldarine"));

                mockMvc.perform(get("/api/sessions/{sessionId}/chat/messages", context.sessionId())
                                .with(Objects.requireNonNull(authHelper.bearerToken(PLAYER_TWO_EMAIL, PLAYER_TWO_PASSWORD))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].contentVisible", matchesPattern(".*[^A-Za-z0-9 ,!]+.*")));

                mockMvc.perform(get("/api/sessions/{sessionId}/chat/messages", context.sessionId())
                                .with(Objects.requireNonNull(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].contentVisible").value("For the Seldarine"));
        }

        @Test
        void gmCanSendWithoutCharacterAndPlayersNeedKnowledge() throws Exception {
                SessionContext context = setupSessionWithPlayers();

                mockMvc.perform(post("/api/sessions/{sessionId}/chat/messages", context.sessionId())
                                .with(Objects.requireNonNull(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(chatPayload(null, "DRACONIC", "The wyrm awakes")))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.contentVisible").value("The wyrm awakes"));

                mockMvc.perform(get("/api/sessions/{sessionId}/chat/messages", context.sessionId())
                                .with(Objects.requireNonNull(authHelper.bearerToken(PLAYER_TWO_EMAIL, PLAYER_TWO_PASSWORD))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].contentVisible", matchesPattern(".*[^A-Za-z0-9 ,!]+.*")));
        }

        @Test
        void unauthorizedUserCannotAccessChatAndPlayersMustKnowLanguage() throws Exception {
                SessionContext context = setupSessionWithPlayers();
                registerUser(VIEWER_EMAIL, VIEWER_PASSWORD, "Viewer", "PLAYER");

                mockMvc.perform(get("/api/sessions/{sessionId}/chat/messages", context.sessionId())
                                .with(Objects.requireNonNull(authHelper.bearerToken(VIEWER_EMAIL, VIEWER_PASSWORD))))
                                .andExpect(status().isForbidden());

                mockMvc.perform(post("/api/sessions/{sessionId}/chat/messages", context.sessionId())
                                .with(Objects.requireNonNull(authHelper.bearerToken(PLAYER_TWO_EMAIL, PLAYER_TWO_PASSWORD)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(chatPayload(context.playerTwoCharacterId(), "ELVISH", "Try to cheat")))
                                .andExpect(status().isBadRequest());
        }

        private SessionContext setupSessionWithPlayers() throws Exception {
                registerUser(GM_EMAIL, GM_PASSWORD, "ChatGM", "DM");
                grantRole(GM_EMAIL, "ROLE_GM");
                registerUser(PLAYER_ONE_EMAIL, PLAYER_ONE_PASSWORD, "ElvishPlayer", "PLAYER");
                registerUser(PLAYER_TWO_EMAIL, PLAYER_TWO_PASSWORD, "CommonPlayer", "PLAYER");

                Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "Chat World");
                Long campaignId = createCampaign(GM_EMAIL, GM_PASSWORD, worldId, "Chat Campaign");
                Long sessionId = createSession(GM_EMAIL, GM_PASSWORD, campaignId, "Chat Session");

                Long characterOneId = createCharacter(PLAYER_ONE_EMAIL, PLAYER_ONE_PASSWORD, worldId, "Lia", true,
                                new String[] { "COMMON", "ELVISH" });
                Long characterTwoId = createCharacter(PLAYER_TWO_EMAIL, PLAYER_TWO_PASSWORD, worldId, "Dorn", true,
                                new String[] { "COMMON" });

                Long joinOneId = requestJoin(PLAYER_ONE_EMAIL, PLAYER_ONE_PASSWORD, campaignId, characterOneId);
                approveJoin(GM_EMAIL, GM_PASSWORD, campaignId, joinOneId);
                Long joinTwoId = requestJoin(PLAYER_TWO_EMAIL, PLAYER_TWO_PASSWORD, campaignId, characterTwoId);
                approveJoin(GM_EMAIL, GM_PASSWORD, campaignId, joinTwoId);

                return new SessionContext(sessionId, characterOneId, characterTwoId);
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
                                                "{\"worldId\":%d,\"name\":\"%s\",\"description\":\"desc\",\"status\":\"ACTIVE\"}",
                                                worldId,
                                                name)))
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
                                                "{\"title\":\"%s\",\"sessionNumber\":1,\"sessionDate\":\"2024-01-01\",\"notes\":\"notes\"}",
                                                title)))
                                .andExpect(status().isCreated())
                                .andReturn();
                JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
                return node.get("id").asLong();
        }

        private Long createCharacter(String email, String password, Long worldId, String name, boolean visible,
                        String[] languages)
                        throws Exception {
                ObjectNode payload = characterPayloadNode(worldId, name, visible);
                ArrayNode knownLanguages = payload.putArray("knownLanguages");
                for (String language : languages) {
                        knownLanguages.add(language);
                }
                MvcResult result = mockMvc.perform(post("/api/characters")
                                .with(authHelper.bearerToken(email, password))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isCreated())
                                .andReturn();
                JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
                return node.get("id").asLong();
        }

        private ObjectNode characterPayloadNode(Long worldId, String name, boolean visible) {
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
                node.put("gmNotes", "gm notes");
                node.put("isVisibleToPlayers", visible);
                return node;
        }

        private Long requestJoin(String email, String password, Long campaignId, Long characterId) throws Exception {
                MvcResult result = mockMvc.perform(post("/api/campaigns/{campaignId}/join-requests", campaignId)
                                .with(authHelper.bearerToken(email, password))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.format("{\"characterId\":%d}", characterId)))
                                .andExpect(status().isCreated())
                                .andReturn();
                JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
                return node.get("id").asLong();
        }

        private void approveJoin(String email, String password, Long campaignId, Long requestId) throws Exception {
                mockMvc.perform(
                                post("/api/campaigns/{campaignId}/join-requests/{campaignPlayerId}/approve", campaignId,
                                                requestId)
                                                .with(authHelper.bearerToken(email, password)))
                                .andExpect(status().isOk());
        }

        private String chatPayload(Long senderCharacterId, String language, String content) throws Exception {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("content", content);
                node.put("language", language);
                if (senderCharacterId != null) {
                        node.put("senderCharacterId", senderCharacterId);
                }
                node.put("messageType", "IC");
                return objectMapper.writeValueAsString(node);
        }

        private record SessionContext(Long sessionId, Long playerOneCharacterId, Long playerTwoCharacterId) {
        }
}
