package it.univ.ddmanager.dice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.univ.ddmanager.dice.dto.DiceRollResponse;
import it.univ.ddmanager.dice.service.DiceService;
import it.univ.ddmanager.support.TestAuthenticationHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiceController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for this test
class DiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiceService diceService;

    // We need to mock this bean even if security is disabled because it might be
    // injected elsewhere
    @MockBean
    private TestAuthenticationHelper authHelper;

    @Test
    void rollD4_shouldReturnResult() throws Exception {
        DiceRollResponse response = new DiceRollResponse(4, 4, java.time.Instant.now());
        when(diceService.rollD4()).thenReturn(response);

        mockMvc.perform(get("/api/dice/d4")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(4))
                .andExpect(jsonPath("$.sides").value(4));
    }

    @Test
    void rollD6_shouldReturnResult() throws Exception {
        DiceRollResponse response = new DiceRollResponse(6, 6, java.time.Instant.now());
        when(diceService.rollD6()).thenReturn(response);

        mockMvc.perform(get("/api/dice/d6")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(6))
                .andExpect(jsonPath("$.sides").value(6));
    }

    @Test
    void rollD8_shouldReturnResult() throws Exception {
        DiceRollResponse response = new DiceRollResponse(8, 8, java.time.Instant.now());
        when(diceService.rollD8()).thenReturn(response);

        mockMvc.perform(get("/api/dice/d8")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(8))
                .andExpect(jsonPath("$.sides").value(8));
    }

    @Test
    void rollD10_shouldReturnResult() throws Exception {
        DiceRollResponse response = new DiceRollResponse(10, 10, java.time.Instant.now());
        when(diceService.rollD10()).thenReturn(response);

        mockMvc.perform(get("/api/dice/d10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(10))
                .andExpect(jsonPath("$.sides").value(10));
    }

    @Test
    void rollD12_shouldReturnResult() throws Exception {
        DiceRollResponse response = new DiceRollResponse(12, 12, java.time.Instant.now());
        when(diceService.rollD12()).thenReturn(response);

        mockMvc.perform(get("/api/dice/d12")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(12))
                .andExpect(jsonPath("$.sides").value(12));
    }

    @Test
    void rollD20_shouldReturnResult() throws Exception {
        DiceRollResponse response = new DiceRollResponse(20, 20, java.time.Instant.now());
        when(diceService.rollD20()).thenReturn(response);

        mockMvc.perform(get("/api/dice/d20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(20))
                .andExpect(jsonPath("$.sides").value(20));
    }
}
