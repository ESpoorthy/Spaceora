package com.spaceora.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spaceora.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniqueEmail() {
        return "test_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private Map<String, Object> validPayload(String email) {
        return Map.of(
                "firstName", "Test",
                "lastName", "User",
                "email", email,
                "password", "SecurePass1!"
        );
    }

    private String registerAndGetToken(String email) throws Exception {
        // Register
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPayload(email))))
                .andExpect(status().isCreated());

        // Login
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "SecurePass1!"))))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return (String) body.get("token");
    }

    // 1. register_valid_returns201AndProfileShape
    @Test
    void register_validRequest_returns201AndProfileShape() throws Exception {
        String email = uniqueEmail();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPayload(email))))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertThat(body.get("id")).isNotNull();
        assertThat(body.get("email")).isEqualTo(email);
        assertThat(body.get("role")).isEqualTo("CUSTOMER");
        assertThat(body.containsKey("passwordHash")).isFalse();
        assertThat(body.containsKey("password")).isFalse();
    }

    // 2. register_duplicateEmail_returns409
    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPayload(email))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPayload(email))))
                .andExpect(status().isConflict());
    }

    // 3. register_weakPassword_returns400
    @Test
    void register_weakPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "firstName", "T", "lastName", "U",
                        "email", uniqueEmail(), "password", "short"))))
                .andExpect(status().isBadRequest());
    }

    // 4. register_invalidEmail_returns400
    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "firstName", "T", "lastName", "U",
                        "email", "not-an-email", "password", "SecurePass1!"))))
                .andExpect(status().isBadRequest());
    }

    // 5. register_adminRoleAttempt_returns403
    @Test
    void register_adminRoleAttempt_returns403() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "firstName", "A", "lastName", "B",
                        "email", uniqueEmail(), "password", "SecurePass1!",
                        "role", "ADMIN"))))
                .andExpect(status().isForbidden());
    }

    // 6. login_validCredentials_returns200WithToken
    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPayload(email))))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "SecurePass1!"))))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertThat(body.get("token")).isNotNull();
        assertThat(body.get("userId")).isNotNull();
        assertThat(body.get("role")).isEqualTo("CUSTOMER");
    }

    // 7. login_wrongPassword_returns401
    @Test
    void login_wrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPayload(email))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "WrongPass1!"))))
                .andExpect(status().isUnauthorized());
    }

    // 8. login_unknownEmail_returns401
    @Test
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "nobody_" + UUID.randomUUID() + "@example.com",
                        "password", "SecurePass1!"))))
                .andExpect(status().isUnauthorized());
    }

    // 9. getMe_withValidToken_returns200Profile
    @Test
    void getMe_withValidToken_returns200Profile() throws Exception {
        String email = uniqueEmail();
        String token = registerAndGetToken(email);

        MvcResult result = mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertThat(body.get("email")).isEqualTo(email);
    }

    // 10. getMe_withoutToken_returns401
    @Test
    void getMe_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // 11. getMe_withInvalidToken_returns401
    @Test
    void getMe_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer this.is.garbage.token.xyz"))
                .andExpect(status().isUnauthorized());
    }

    // 12. updateProfile_withValidToken_returns200
    @Test
    void updateProfile_withValidToken_returns200() throws Exception {
        String email = uniqueEmail();
        String token = registerAndGetToken(email);

        MvcResult result = mockMvc.perform(put("/api/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("firstName", "Updated"))))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertThat(body.get("firstName")).isEqualTo("Updated");
    }

    // 13. protectedEndpoint_noToken_returns401
    @Test
    void protectedEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
