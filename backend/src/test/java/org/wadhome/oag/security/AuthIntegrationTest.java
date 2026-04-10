package org.wadhome.oag.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.wadhome.oag.api.dto.LoginRequest;
import org.wadhome.oag.api.dto.PasswordChangeRequest;
import org.wadhome.oag.service.DataInitializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginWithCorrectPasswordReturnsToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(DataInitializer.DEFAULT_PASSWORD))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("wrongpassword"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointRequiresToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/images"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointAcceptsValidToken() throws Exception {
        // GET /api/v1/admin/images not yet implemented; verify security layer passes it (not 401/403)
        String token = getToken();
        int status = mockMvc.perform(get("/api/v1/admin/images")
                .header("Authorization", "Bearer " + token))
            .andReturn().getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void passwordChangeWithWrongCurrentPasswordFails() throws Exception {
        String token = getToken();
        mockMvc.perform(put("/api/v1/admin/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new PasswordChangeRequest("WrongCurrentPassword", "NewPassword123!"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangeWithTooShortNewPasswordFails() throws Exception {
        String token = getToken();
        mockMvc.perform(put("/api/v1/admin/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new PasswordChangeRequest(DataInitializer.DEFAULT_PASSWORD, "short"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void publicEndpointAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/public/themes"))
            .andExpect(status().isOk());
    }

    private String getToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(DataInitializer.DEFAULT_PASSWORD))))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }
}
