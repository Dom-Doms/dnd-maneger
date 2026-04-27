package it.univ.ddmanager.world;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
class WorldCampaignSmokeTest {

        private static final String GM_EMAIL = "gm_smoke@example.com";
        private static final String GM_PASSWORD = "SmokePassword123!";
        private static final String PLAYER_EMAIL = "player_smoke@example.com";
        private static final String PLAYER_PASSWORD = "PlayerPass123!";
        private static final String VIEWER_EMAIL = "viewer_smoke@example.com";
        private static final String VIEWER_PASSWORD = "ViewerPass123!";

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
        void createWorld_and_listWorlds_withGM() throws Exception {
                registerAndPromoteGm();

                MvcResult creationResult = mockMvc.perform(post("/api/worlds")
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(worldPayload("World Smoke", "World per smoke test")))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("World Smoke"))
                                .andExpect(jsonPath("$.ownerNickname").value("SmokeGM"))
                                .andExpect(jsonPath("$.isPublic").value(false))
                                .andExpect(jsonPath("$.campaignCount").value(0))
                                .andReturn();
                JsonNode createdWorld = objectMapper.readTree(creationResult.getResponse().getContentAsString());
                long worldId = createdWorld.get("id").asLong();

                mockMvc.perform(get("/api/worlds").with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(worldId))
                                .andExpect(jsonPath("$[0].name").value("World Smoke"))
                                .andExpect(jsonPath("$[0].isPublic").value(false))
                                .andExpect(jsonPath("$[0].campaignCount").value(0))
                                .andExpect(jsonPath("$[0].ownerNickname").value("SmokeGM"));
        }

        @Test
        void player_canDiscoverPublicWorlds() throws Exception {
                registerAndPromoteGm();
                registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "PublicPlayer", "PLAYER");

                long publicWorldId = createWorld("Public World", true);
                long privateWorldId = createWorld("Private World", false);

                mockMvc.perform(get("/api/worlds/public").with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].id").value(publicWorldId))
                                .andExpect(jsonPath("$[0].isPublic").value(true));

                mockMvc.perform(get("/api/worlds/public/{id}", publicWorldId)
                                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(publicWorldId))
                                .andExpect(jsonPath("$.isPublic").value(true));

                mockMvc.perform(get("/api/worlds/public/{id}", privateWorldId)
                                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                                .andExpect(status().isNotFound());

                mockMvc.perform(get("/api/worlds/public/{id}", privateWorldId)
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(privateWorldId))
                                .andExpect(jsonPath("$.isPublic").value(false));
        }

        @Test
        void createWorld_shouldReturn401_withoutAuth() throws Exception {
                mockMvc.perform(post("/api/worlds")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(worldPayload("World Unauthorized", "N/A")))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        void createCampaign_withGM_shouldReturn201() throws Exception {
                registerAndPromoteGm();
                Long worldId = createWorld("Campaign World");

                mockMvc.perform(post("/api/campaigns")
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(campaignPayload(worldId, "Campagna Login Test", "Campagna di prova")))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("Campagna Login Test"))
                                .andExpect(jsonPath("$.worldId").value(worldId))
                                .andExpect(jsonPath("$.ownerNickname").value("SmokeGM"));
        }

        @Test
        void createSession_withGM_shouldReturn201() throws Exception {
                registerAndPromoteGm();
                Long worldId = createWorld("Session World");
                Long campaignId = createCampaign(worldId, "Campagna Sessione");

                mockMvc.perform(post("/api/campaigns/{campaignId}/sessions", campaignId)
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(sessionPayload("Sessione 1", 1, "Prima sessione")))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.campaignId").value(campaignId))
                                .andExpect(jsonPath("$.ownerNickname").value("SmokeGM"))
                                .andExpect(jsonPath("$.sessionNumber").value(1));
        }

        @Test
        void createWorld_withPlayer_shouldReturn403() throws Exception {
                registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "Player", "PLAYER");

                mockMvc.perform(post("/api/worlds")
                                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(worldPayload("Player World", "Should fail")))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        void createCampaign_withPlayer_shouldReturn403() throws Exception {
                registerAndPromoteGm();
                Long worldId = createWorld("GM World");
                registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "Player", "PLAYER");

                mockMvc.perform(post("/api/campaigns")
                                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(campaignPayload(worldId, "Player Campaign", "Should fail")))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        void createSession_withViewer_shouldReturn403() throws Exception {
                registerAndPromoteGm();
                Long worldId = createWorld("GM World for Viewer");
                Long campaignId = createCampaign(worldId, "GM Campaign");
                registerUser(VIEWER_EMAIL, VIEWER_PASSWORD, "Viewer", "VIEWER");

                mockMvc.perform(post("/api/campaigns/{campaignId}/sessions", campaignId)
                                .with(authHelper.bearerToken(VIEWER_EMAIL, VIEWER_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(sessionPayload("Viewer Session", 1, "Should fail")))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        void createCampaign_withDuplicateNameInSameWorld_shouldReturn400() throws Exception {
                registerAndPromoteGm();
                Long worldId = createWorld("World With Campaigns");

                mockMvc.perform(post("/api/campaigns")
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(campaignPayload(worldId, "Dup Campaign", "Prima")))
                                .andExpect(status().isCreated());

                mockMvc.perform(post("/api/campaigns")
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(campaignPayload(worldId, "Dup Campaign", "Seconda")))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value("Campaign name already exists in this world: Dup Campaign"));
        }

        @Test
        void gm_canUpdateAndDeleteOwnWorld() throws Exception {
                registerAndPromoteGm();
                Long worldId = createWorld("Updatable World");

                mockMvc.perform(put("/api/worlds/{id}", worldId)
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(worldPayload("Updated Name", "Updated description")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Updated Name"));

                mockMvc.perform(delete("/api/worlds/{id}", worldId)
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                                .andExpect(status().isNoContent());

                mockMvc.perform(get("/api/worlds/{id}", worldId).with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                                .andExpect(status().isNotFound());
        }

        @Test
        void player_cannotUpdateOrDeleteWorld() throws Exception {
                registerAndPromoteGm();
                Long worldId = createWorld("GM World");
                registerUser(PLAYER_EMAIL, PLAYER_PASSWORD, "Player", "PLAYER");

                mockMvc.perform(put("/api/worlds/{id}", worldId)
                                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(worldPayload("Player Update", "Should fail")))
                                .andExpect(status().isForbidden());

                mockMvc.perform(delete("/api/worlds/{id}", worldId)
                                .with(authHelper.bearerToken(PLAYER_EMAIL, PLAYER_PASSWORD)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void worlds_shouldReturn401_withoutAuth() throws Exception {
                mockMvc.perform(get("/api/worlds"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void gm_canOnlySeeMyWorlds_viaMyEndpoint() throws Exception {
                // Register DM1 (default GM)
                registerAndPromoteGm();
                Long dm1WorldId = createWorld("DM1 World");

                // Register DM2
                String dm2Email = "dm2@example.com";
                String dm2Pass = "Password123!";
                registerUser(dm2Email, dm2Pass, "DM2", "DM");
                grantRole(dm2Email, "ROLE_GM");

                // Create world for DM2
                MvcResult result = mockMvc.perform(post("/api/worlds")
                                .with(authHelper.bearerToken(dm2Email, dm2Pass))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(worldPayload("DM2 World", "Description")))
                                .andExpect(status().isCreated())
                                .andReturn();
                JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
                Long dm2WorldId = node.get("id").asLong();

                // DM1 should strictly see only their own world via /api/worlds/my
                mockMvc.perform(get("/api/worlds/my")
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].id").value(dm1WorldId))
                                .andExpect(jsonPath("$[0].name").value("DM1 World"));

                // DM2 should strictly see only their own world via /api/worlds/my
                mockMvc.perform(get("/api/worlds/my")
                                .with(authHelper.bearerToken(dm2Email, dm2Pass)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].id").value(dm2WorldId))
                                .andExpect(jsonPath("$[0].name").value("DM2 World"));
        }

        @Test
        void gm_canOnlySeeMyCampaigns_viaMyEndpoint() throws Exception {
                // Register DM1 (default GM)
                registerAndPromoteGm();
                Long dm1WorldId = createWorld("DM1 World C");

                // Register DM2
                String dm2Email = "dm2_c@example.com";
                String dm2Pass = "Password123!";
                registerUser(dm2Email, dm2Pass, "DM2_C", "DM");
                grantRole(dm2Email, "ROLE_GM");

                // Create world for DM2
                MvcResult worldResult = mockMvc.perform(post("/api/worlds")
                                .with(authHelper.bearerToken(dm2Email, dm2Pass))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(worldPayload("DM2 World C", "Description")))
                                .andExpect(status().isCreated())
                                .andReturn();
                JsonNode worldNode = objectMapper.readTree(worldResult.getResponse().getContentAsString());
                Long dm2WorldId = worldNode.get("id").asLong();

                // 3. DM1 creates Campaign1 in World1
                mockMvc.perform(post("/api/campaigns")
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "worldId": %d,
                                                    "name": "DM1 Campaign",
                                                    "description": "Desc",
                                                    "status": "ACTIVE"
                                                }
                                                """.formatted(dm1WorldId)))
                                .andExpect(status().isCreated());

                // 4. DM2 creates Campaign2 in World2
                mockMvc.perform(post("/api/campaigns")
                                .with(authHelper.bearerToken(dm2Email, dm2Pass))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "worldId": %d,
                                                    "name": "DM2 Campaign",
                                                    "description": "Desc",
                                                    "status": "ACTIVE"
                                                }
                                                """.formatted(dm2WorldId)))
                                .andExpect(status().isCreated());

                // 5. DM1 calls /api/campaigns/my -> expects 1 campaign (DM1 Campaign)
                mockMvc.perform(get("/api/campaigns/my")
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].name", is("DM1 Campaign")));

                // 6. DM2 calls /api/campaigns/my -> expects 1 campaign (DM2 Campaign)
                mockMvc.perform(get("/api/campaigns/my")
                                .with(authHelper.bearerToken(dm2Email, dm2Pass)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].name", is("DM2 Campaign")));
        }

        private void registerAndPromoteGm() throws Exception {
                registerUser(GM_EMAIL, GM_PASSWORD, "SmokeGM", "DM");
                grantRole(GM_EMAIL, "ROLE_GM");
        }

        private void registerUser(String email, String password, String nickname, String role) throws Exception {
                RegisterRequest request = new RegisterRequest(email, password, nickname, role);
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());
        }

        private Long createWorld(String name) throws Exception {
                return createWorld(name, null);
        }

        private Long createWorld(String name, Boolean isPublic) throws Exception {
                MvcResult result = mockMvc.perform(post("/api/worlds")
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(worldPayload(name, "World per campagne", isPublic)))
                                .andExpect(status().isCreated())
                                .andReturn();
                JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
                return node.get("id").asLong();
        }

        private Long createCampaign(Long worldId, String name) throws Exception {
                MvcResult result = mockMvc.perform(post("/api/campaigns")
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(campaignPayload(worldId, name, "Campagna di prova")))
                                .andExpect(status().isCreated())
                                .andReturn();
                JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
                return node.get("id").asLong();
        }

        private void grantRole(String email, String roleName) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
                Role role = roleRepository.findByName(roleName)
                                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));
                user.getRoles().add(role);
                userRepository.save(user);
        }

        private String worldPayload(String name, String description) {
                return worldPayload(name, description, null);
        }

        private String worldPayload(String name, String description, Boolean isPublic) {
                StringBuilder builder = new StringBuilder();
                builder.append("{\"name\":\"").append(name).append("\",\"description\":\"").append(description)
                                .append("\"");
                if (isPublic != null) {
                        builder.append(",\"isPublic\":").append(isPublic);
                }
                builder.append("}");
                return builder.toString();
        }

        private String campaignPayload(Long worldId, String name, String description) {
                return String.format("{\"worldId\":%d,\"name\":\"%s\",\"description\":\"%s\",\"status\":\"ACTIVE\"}",
                                worldId, name, description);
        }

        private String sessionPayload(String title, int sessionNumber, String notes) {
                return String.format(
                                "{\"title\":\"%s\",\"sessionNumber\":%d,\"sessionDate\":\"2024-01-01\",\"notes\":\"%s\"}",
                                title, sessionNumber, notes);
        }

        @Test
        void gm_canOnlySeeMySessions_viaMyEndpoint() throws Exception {
                // 1. Setup DM1, World1, Campaign1
                registerAndPromoteGm();
                Long dm1WorldId = createWorld("DM1 World S");
                // Create Campaign1
                String dm1CampResponse = mockMvc.perform(post("/api/campaigns")
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "worldId": %d,
                                                    "name": "DM1 Campaign S",
                                                    "description": "Desc",
                                                    "status": "ACTIVE"
                                                }
                                                """.formatted(dm1WorldId)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                Integer dm1CampaignId = com.jayway.jsonpath.JsonPath.read(dm1CampResponse, "$.id");

                // 2. Setup DM2, World2, Campaign2
                String dm2Email = "dm2_s@example.com";
                String dm2Pass = "Password123!";
                registerUser(dm2Email, dm2Pass, "DM2_S", "DM");
                grantRole(dm2Email, "ROLE_GM");

                String dm2WorldResponse = mockMvc.perform(post("/api/worlds")
                                .with(authHelper.bearerToken(dm2Email, dm2Pass))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(worldPayload("DM2 World S", "Description")))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                Integer dm2WorldId = com.jayway.jsonpath.JsonPath.read(dm2WorldResponse, "$.id");

                String dm2CampResponse = mockMvc.perform(post("/api/campaigns")
                                .with(authHelper.bearerToken(dm2Email, dm2Pass))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "worldId": %d,
                                                    "name": "DM2 Campaign S",
                                                    "description": "Desc",
                                                    "status": "ACTIVE"
                                                }
                                                """.formatted(dm2WorldId)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                Integer dm2CampaignId = com.jayway.jsonpath.JsonPath.read(dm2CampResponse, "$.id");

                // 3. DM1 creates Session1
                mockMvc.perform(post("/api/campaigns/{id}/sessions", dm1CampaignId)
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "title": "DM1 Session",
                                                    "sessionNumber": 1,
                                                    "sessionDate": "2024-01-01",
                                                    "notes": "Intro"
                                                }
                                                """))
                                .andExpect(status().isCreated());

                // 4. DM2 creates Session2
                mockMvc.perform(post("/api/campaigns/{id}/sessions", dm2CampaignId)
                                .with(authHelper.bearerToken(dm2Email, dm2Pass))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "title": "DM2 Session",
                                                    "sessionNumber": 1,
                                                    "sessionDate": "2024-01-01",
                                                    "notes": "Intro"
                                                }
                                                """))
                                .andExpect(status().isCreated());

                mockMvc.perform(get("/api/sessions/my")
                                .with(authHelper.bearerToken(dm2Email, dm2Pass)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].title", is("DM2 Session")));
        }

        @Test
        void player_canSeeSessions_whenJoined() throws Exception {
                // 1. Setup GM, World, Campaign
                registerAndPromoteGm();
                Long worldId = createWorld("Joined World");
                Long campaignId = createCampaign(worldId, "Joined Campaign");

                // 2. Setup Player
                String playerEmail = "joined_player@example.com";
                String playerPass = "Pass123!";
                registerUser(playerEmail, playerPass, "JoinedPlayer", "PLAYER");

                // 3. Player creates a Character
                String charPayload = """
                                {
                                    "name": "My Hero",
                                    "race": "Human",
                                    "characterClass": "Fighter",
                                    "level": 1,
                                    "experiencePoints": 0,
                                    "maxHitPoints": 10,
                                    "currentHitPoints": 10,
                                    "strength": 10,
                                    "dexterity": 10,
                                    "constitution": 10,
                                    "intelligence": 10,
                                    "wisdom": 10,
                                    "charisma": 10,
                                    "armorClass": 10,
                                    "language": "COMMON"
                                }
                                """;
                MvcResult charResult = mockMvc.perform(post("/api/characters")
                                .with(authHelper.bearerToken(playerEmail, playerPass))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(charPayload))
                                .andExpect(status().isCreated())
                                .andReturn();
                Integer characterId = com.jayway.jsonpath.JsonPath.read(charResult.getResponse().getContentAsString(),
                                "$.id");

                // 4. Player requests to join Campaign
                String joinPayload = "{\"characterId\": " + characterId + ", \"message\": \"Let me in!\"}";
                MvcResult joinResult = mockMvc.perform(post("/api/campaigns/" + campaignId + "/join-requests")
                                .with(authHelper.bearerToken(playerEmail, playerPass))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(joinPayload))
                                .andExpect(status().isCreated())
                                .andReturn();
                Integer joinRequestId = com.jayway.jsonpath.JsonPath.read(joinResult.getResponse().getContentAsString(),
                                "$.id");

                // 5. GM approves join request
                mockMvc.perform(post("/api/campaigns/" + campaignId + "/join-requests/" + joinRequestId + "/approve")
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", is("APPROVED")))
                                .andExpect(jsonPath("$.knownLanguages", hasItem("COMMON")));

                // 5. GM Approves Request
                mockMvc.perform(post("/api/campaigns/" + campaignId + "/join-requests/" + joinRequestId + "/approve")
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                                .andExpect(status().isOk());

                // 6. GM creates a Session
                createSession(campaignId, "Player Visible Session");

                // 7. Player calls /api/sessions/my -> SHOULD see the session now
                mockMvc.perform(get("/api/sessions/my")
                                .with(authHelper.bearerToken(playerEmail, playerPass)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].title", is("Player Visible Session")));
        }

        private void createSession(Long campaignId, String title) throws Exception {
                mockMvc.perform(post("/api/campaigns/{id}/sessions", campaignId)
                                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "title": "%s",
                                                    "sessionNumber": 1,
                                                    "sessionDate": "2024-01-01",
                                                    "notes": "Intro"
                                                }
                                                """.formatted(title)))
                                .andExpect(status().isCreated());
        }
}
