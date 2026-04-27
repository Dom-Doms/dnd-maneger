package it.univ.ddmanager.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.univ.ddmanager.resource.service.FileStorageService;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SessionResourceIntegrationTest {

    private static final String GM_EMAIL = "gm_res@example.com";
    private static final String GM_PASSWORD = "GmPassword123!";

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

    @MockBean
    private FileStorageService fileStorageService;

    @BeforeEach
    void cleanDatabase() {
        databaseCleanup.truncateAll();
    }

    @Test
    void uploadAndDownloadResourceReference() throws Exception {
        Long sessionId = setupSession();

        // Mock file storage
        String storedFileName = "stored-map.png";
        when(fileStorageService.store(any())).thenReturn(storedFileName);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "map.png",
                MediaType.IMAGE_PNG_VALUE,
                "test image content".getBytes());

        // Upload
        mockMvc.perform(multipart("/api/sessions/{sessionId}/upload", sessionId)
                .file(file)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("map.png"))
                .andExpect(jsonPath("$.fileType").value("IMAGE"))
                .andExpect(jsonPath("$.fileUrl").exists());

        // List resources
        mockMvc.perform(get("/api/sessions/{sessionId}/resources", sessionId)
                .with(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("map.png"));
    }

    @Test
    void downloadFileContent() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "GMUser", "DM");

        // Mock file storage
        String fileName = "stored-map.png";
        // Create a ByteArrayResource with a description to return a filename
        ByteArrayResource resource = new ByteArrayResource("test content".getBytes()) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        when(fileStorageService.loadAsResource(fileName)).thenReturn(resource);

        mockMvc.perform(get("/api/resources/file/{fileName}", fileName)
                .with(Objects.requireNonNull(authHelper.bearerToken(GM_EMAIL, GM_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(content().string("test content"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"" + fileName + "\""));
    }

    private Long setupSession() throws Exception {
        registerUser(GM_EMAIL, GM_PASSWORD, "GMUser", "DM");
        grantRole(GM_EMAIL, "ROLE_GM");
        Long worldId = createWorld(GM_EMAIL, GM_PASSWORD, "ResWorld");
        Long campaignId = createCampaign(GM_EMAIL, GM_PASSWORD, worldId, "ResCampaign");
        return createSession(GM_EMAIL, GM_PASSWORD, campaignId, "ResSession");
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
}
