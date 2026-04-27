package it.univ.ddmanager;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.univ.ddmanager.support.TestAuthenticationHelper;
import it.univ.ddmanager.user.dto.RegisterRequest;
import it.univ.ddmanager.user.model.Role;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.support.TestDatabaseCleanup;
import it.univ.ddmanager.user.repository.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthIntegrationTest {

    private static final String REGISTER_EMAIL = "register@example.com";
    private static final String LOGIN_EMAIL = "login@example.com";
    private static final String LOGIN_INVALID_EMAIL = "login-invalid@example.com";
    private static final String LOGIN_MISSING_EMAIL = "missing@example.com";
    private static final String ME_EMAIL = "me@example.com";
    private static final String PLAYER_EMAIL = "player_role@example.com";
    private static final String DM_EMAIL = "dm_role@example.com";
    private static final String VIEWER_EMAIL = "viewer_role@example.com";
    private static final String INVALID_ROLE_EMAIL = "invalid_role@example.com";
    private static final String DEFAULT_PASSWORD = "Secret123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestDatabaseCleanup databaseCleanup;

    @Autowired
    private TestAuthenticationHelper authHelper;

    @BeforeEach
    void cleanUp() {
        databaseCleanup.truncateAll();
    }

    @Test
    void registerUser_shouldCreateUserAndEncodePassword() throws Exception {
        registerUser(REGISTER_EMAIL, DEFAULT_PASSWORD, "RegisterUser", "PLAYER");

        User user = userRepository.findByEmail(REGISTER_EMAIL).orElseThrow();
        assertNotEquals(DEFAULT_PASSWORD, user.getPassword());
        assertTrue(user.getPassword().startsWith("$2"));
    }

    @Test
    void login_shouldReturnAuthResponse_whenCredentialsValid() throws Exception {
        registerUser(LOGIN_EMAIL, DEFAULT_PASSWORD, "LoginUser", "PLAYER");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(LOGIN_EMAIL, DEFAULT_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value(LOGIN_EMAIL))
            .andExpect(jsonPath("$.user.nickname").value("LoginUser"));
    }

    @Test
    void login_shouldReturn401_whenCredentialsInvalid() throws Exception {
        registerUser(LOGIN_INVALID_EMAIL, DEFAULT_PASSWORD, "LoginFail", "PLAYER");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(LOGIN_INVALID_EMAIL, "WrongPassword!")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_shouldReturn404_whenUserNotFound() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(LOGIN_MISSING_EMAIL, DEFAULT_PASSWORD)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("User not found: " + LOGIN_MISSING_EMAIL));
    }

    @Test
    void me_shouldReturnUser_whenAuthenticated() throws Exception {
        registerUser(ME_EMAIL, DEFAULT_PASSWORD, "MeUser", "DM");

        mockMvc.perform(get("/api/auth/me")
                .with(authHelper.bearerToken(ME_EMAIL)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(ME_EMAIL))
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.roles", hasItem("ROLE_GM")));
    }

    @Test
    void me_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_asPlayer_shouldAssignRolePlayer() throws Exception {
        registerUser(PLAYER_EMAIL, DEFAULT_PASSWORD, "PlayerUser", "PLAYER");
        User user = userRepository.findByEmail(PLAYER_EMAIL).orElseThrow();
        assertEquals(Set.of("ROLE_PLAYER"), extractRoleNames(user));
    }

    @Test
    void register_asDm_shouldAssignRoleGm() throws Exception {
        registerUser(DM_EMAIL, DEFAULT_PASSWORD, "DungeonMaster", "DM");
        User user = userRepository.findByEmail(DM_EMAIL).orElseThrow();
        assertEquals(Set.of("ROLE_GM"), extractRoleNames(user));
    }

    @Test
    void register_asViewer_shouldAssignRoleViewer() throws Exception {
        registerUser(VIEWER_EMAIL, DEFAULT_PASSWORD, "ViewerUser", "VIEWER");
        User user = userRepository.findByEmail(VIEWER_EMAIL).orElseThrow();
        assertEquals(Set.of("ROLE_VIEWER"), extractRoleNames(user));
    }

    @Test
    void register_withExistingEmail_shouldReturn409() throws Exception {
        registerUser(REGISTER_EMAIL, DEFAULT_PASSWORD, "FirstUser", "PLAYER");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s","nickname":"Duplicate","role":"PLAYER"}
                    """.formatted(REGISTER_EMAIL, DEFAULT_PASSWORD)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    void register_withInvalidRole_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s","nickname":"BadRole","role":"KING"}
                    """.formatted(INVALID_ROLE_EMAIL, DEFAULT_PASSWORD)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Invalid role: KING"));
    }

    @Test
    void me_canUpdateNickname() throws Exception {
        registerUser(REGISTER_EMAIL, DEFAULT_PASSWORD, "OldNick", "PLAYER");

        mockMvc.perform(put("/api/auth/me")
                .with(authHelper.bearerToken(REGISTER_EMAIL))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nickname":"NewNick"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nickname").value("NewNick"));

        User updated = userRepository.findByEmail(REGISTER_EMAIL).orElseThrow();
        assertEquals("NewNick", updated.getNickname());
    }

    @Test
    void me_canUpdatePassword() throws Exception {
        registerUser(PLAYER_EMAIL, DEFAULT_PASSWORD, "PwdUser", "PLAYER");

        mockMvc.perform(put("/api/auth/me")
                .with(authHelper.bearerToken(PLAYER_EMAIL))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"password":"NewSecret123"}
                    """))
            .andExpect(status().isOk());

        User updated = userRepository.findByEmail(PLAYER_EMAIL).orElseThrow();
        assertNotEquals("NewSecret123", updated.getPassword());
        assertTrue(updated.getPassword().startsWith("$2"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"NewSecret123"}
                    """.formatted(PLAYER_EMAIL)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    private void registerUser(String email, String password, String nickname, String role) throws Exception {
        RegisterRequest request = new RegisterRequest(email, password, nickname, role);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    private Set<String> extractRoleNames(User user) {
        return user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet());
    }
}
