package com.spaceora.space.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spaceora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * Integration tests for SpaceController endpoints.
 * Uses H2 in-memory DB (application-test.yml) with Hibernate create-drop schema.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String customerToken;
    private String ownerToken;

    private String uniqueEmail() {
        return "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.com";
    }

    /**
     * Register a user with the given role and return the JWT token.
     */
    private String registerAndLogin(String email, Role role) throws Exception {
        Map<String, Object> registerPayload = Map.of(
                "firstName", "Test",
                "lastName", "User",
                "email", email,
                "password", "SecurePass1!",
                "role", role.name()
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "SecurePass1!"))))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
        return (String) body.get("token");
    }

    private Map<String, Object> buildSpacePayload() {
        return Map.of(
                "name", "Test Space",
                "description", "A nice coworking space",
                "type", "COWORKING",
                "city", "London",
                "address", "1 Test Street",
                "hourlyRate", 15.00,
                "capacity", 10
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        customerToken = registerAndLogin(uniqueEmail(), Role.CUSTOMER);
        ownerToken = registerAndLogin(uniqueEmail(), Role.OWNER);
    }

    // (a) GET /api/spaces returns 200 without authentication
    @Test
    void getSpaces_public_returns200() throws Exception {
        mockMvc.perform(get("/api/spaces"))
                .andExpect(status().isOk());
    }

    // (b) POST /api/spaces with OWNER JWT and valid body returns 201
    @Test
    void createSpace_asOwner_returns201() throws Exception {
        mockMvc.perform(post("/api/spaces")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSpacePayload())))
                .andExpect(status().isCreated());
    }

    // (c) POST /api/spaces with CUSTOMER JWT returns 403
    @Test
    void createSpace_asCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/spaces")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSpacePayload())))
                .andExpect(status().isForbidden());
    }

    // (d) POST /api/spaces with no token returns 401
    @Test
    void createSpace_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSpacePayload())))
                .andExpect(status().isUnauthorized());
    }

    // (e) POST /api/spaces with invalid/missing required fields returns 400
    @Test
    void createSpace_invalidData_returns400() throws Exception {
        // Missing required fields: name, type, city, address, hourlyRate, capacity
        Map<String, Object> badPayload = Map.of("description", "only description");

        mockMvc.perform(post("/api/spaces")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badPayload)))
                .andExpect(status().isBadRequest());
    }

    // (f) GET /api/spaces/{id} for existing space returns 200 with correct name
    @Test
    void getById_exists_returns200() throws Exception {
        // Create a space first
        MvcResult createResult = mockMvc.perform(post("/api/spaces")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSpacePayload())))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        Number spaceId = (Number) created.get("id");

        MvcResult getResult = mockMvc.perform(get("/api/spaces/" + spaceId))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(getResult.getResponse().getContentAsString(), Map.class);
        assertThat(body.get("name")).isEqualTo("Test Space");
    }

    // (g) GET /api/spaces/{id} for unknown id returns 404
    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/spaces/999999"))
                .andExpect(status().isNotFound());
    }

    // (h) PUT /api/spaces/{id} with owner's JWT returns 200 with updated name
    @Test
    void updateSpace_asOwner_returns200() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/spaces")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSpacePayload())))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        Number spaceId = (Number) created.get("id");

        Map<String, Object> updatePayload = Map.of("name", "Updated Space Name");

        MvcResult updateResult = mockMvc.perform(put("/api/spaces/" + spaceId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(updateResult.getResponse().getContentAsString(), Map.class);
        assertThat(body.get("name")).isEqualTo("Updated Space Name");
    }

    // (i) PUT /api/spaces/{id} with customer JWT returns 403
    @Test
    void updateSpace_asCustomer_returns403() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/spaces")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSpacePayload())))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        Number spaceId = (Number) created.get("id");

        Map<String, Object> updatePayload = Map.of("name", "Hijacked Name");

        mockMvc.perform(put("/api/spaces/" + spaceId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isForbidden());
    }

    // (j) PUT /api/spaces/{id} with a different owner's JWT returns 403
    @Test
    void updateSpace_asOwner_otherSpace_returns403() throws Exception {
        // Create space as owner1 (ownerToken)
        MvcResult createResult = mockMvc.perform(post("/api/spaces")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSpacePayload())))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        Number spaceId = (Number) created.get("id");

        // Register a second owner
        String owner2Token = registerAndLogin(uniqueEmail(), Role.OWNER);

        Map<String, Object> updatePayload = Map.of("name", "Owner2 Hijack");

        mockMvc.perform(put("/api/spaces/" + spaceId)
                        .header("Authorization", "Bearer " + owner2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isForbidden());
    }

    // (k) DELETE /api/spaces/{id} with owner JWT returns 204
    @Test
    void deleteSpace_asOwner_returns204() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/spaces")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSpacePayload())))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        Number spaceId = (Number) created.get("id");

        mockMvc.perform(delete("/api/spaces/" + spaceId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
    }

    // (l) DELETE /api/spaces/{id} with customer JWT returns 403
    @Test
    void deleteSpace_asCustomer_returns403() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/spaces")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSpacePayload())))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        Number spaceId = (Number) created.get("id");

        mockMvc.perform(delete("/api/spaces/" + spaceId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    // (m) DELETE /api/spaces/999999 with owner JWT returns 404
    @Test
    void deleteSpace_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/spaces/999999")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    // (n) GET /api/spaces/owner/me with OWNER JWT returns 200 and own spaces
    @Test
    void getOwnerSpaces_asOwner_returns200() throws Exception {
        // Create a space as this owner
        mockMvc.perform(post("/api/spaces")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSpacePayload())))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/spaces/owner/me")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();

        // Response should be a JSON array with at least one entry
        Object body = objectMapper.readValue(result.getResponse().getContentAsString(), Object.class);
        assertThat(body).isInstanceOf(java.util.List.class);
        assertThat((java.util.List<?>) body).isNotEmpty();
    }

    // (o) GET /api/spaces/owner/me with CUSTOMER JWT returns 403
    @Test
    void getOwnerSpaces_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/spaces/owner/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    // (p) GET /api/spaces/owner/me with no token returns 401 or 403
    // The security filter chain permits all GET /api/spaces/**, so an anonymous
    // request reaches the @PreAuthorize guard which returns 403 (Access Denied).
    @Test
    void getOwnerSpaces_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/spaces/owner/me"))
                .andExpect(status().isForbidden());
    }

    // (q) GET /api/spaces with an invalid sort field returns 400
    @Test
    void searchSpaces_invalidSortField_returns400() throws Exception {
        mockMvc.perform(get("/api/spaces").param("sort", "injectedField,asc"))
                .andExpect(status().isBadRequest());
    }

    // (r) GET /api/spaces with a valid sort field (hourlyRate) returns 200
    @Test
    void searchSpaces_validSortField_returns200() throws Exception {
        mockMvc.perform(get("/api/spaces").param("sort", "hourlyRate,desc"))
                .andExpect(status().isOk());
    }
}
