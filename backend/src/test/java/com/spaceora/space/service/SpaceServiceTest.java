package com.spaceora.space.service;

import com.spaceora.exception.AuthorizationException;
import com.spaceora.exception.ResourceNotFoundException;
import com.spaceora.space.dto.SpaceDto;
import com.spaceora.space.dto.SpaceRequestDto;
import com.spaceora.space.dto.SpaceUpdateRequestDto;
import com.spaceora.space.entity.Space;
import com.spaceora.space.entity.SpaceType;
import com.spaceora.space.repository.SpaceImageRepository;
import com.spaceora.space.repository.SpaceRepository;
import com.spaceora.user.entity.Role;
import com.spaceora.user.entity.User;
import com.spaceora.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceServiceTest {

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private SpaceImageRepository spaceImageRepository;

    @Mock
    private UserRepository userRepository;

    private SpaceService spaceService;

    @BeforeEach
    void setUp() {
        spaceService = new SpaceService(spaceRepository, spaceImageRepository, userRepository);
    }

    // ---- helpers ----

    private User buildUser(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .email(email)
                .passwordHash("hashed")
                .firstName("Test")
                .lastName("User")
                .role(role)
                .createdAt(Instant.now())
                .build();
    }

    private Space buildSpace(Long spaceId, Long ownerId, String name) {
        return Space.builder()
                .id(spaceId)
                .ownerId(ownerId)
                .name(name)
                .description("A description")
                .type(SpaceType.COWORKING)
                .city("London")
                .address("1 Test Street")
                .hourlyRate(new BigDecimal("10.00"))
                .capacity(10)
                .isActive(true)
                .images(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private SpaceRequestDto buildRequest(String name) {
        return new SpaceRequestDto(
                name,
                "A description",
                SpaceType.COWORKING,
                "London",
                "1 Test Street",
                new BigDecimal("10.00"),
                null,
                10
        );
    }

    // (a) create_asOwner_returnsSpaceDto
    @Test
    void create_asOwner_returnsSpaceDto() {
        User owner = buildUser(1L, "owner@test.com", Role.OWNER);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));

        Space saved = buildSpace(10L, 1L, "My Space");
        when(spaceRepository.save(any(Space.class))).thenReturn(saved);

        SpaceDto result = spaceService.create(buildRequest("My Space"), "owner@test.com");

        assertThat(result).isNotNull();
        assertThat(result.ownerId()).isEqualTo(1L);
    }

    // (b) create_asCustomer_throwsAuthorizationException
    @Test
    void create_asCustomer_throwsAuthorizationException() {
        User customer = buildUser(2L, "customer@test.com", Role.CUSTOMER);
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> spaceService.create(buildRequest("Space"), "customer@test.com"))
                .isInstanceOf(AuthorizationException.class);
    }

    // (c) update_asOwner_ownSpace_returnsUpdated
    @Test
    void update_asOwner_ownSpace_returnsUpdated() {
        User owner = buildUser(1L, "owner@test.com", Role.OWNER);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));

        Space space = buildSpace(10L, 1L, "Old Name");
        when(spaceRepository.findById(10L)).thenReturn(Optional.of(space));
        when(spaceRepository.save(any(Space.class))).thenAnswer(inv -> inv.getArgument(0));

        SpaceUpdateRequestDto dto = new SpaceUpdateRequestDto(
                "New Name", null, null, null, null, null, null, null, null);

        SpaceDto result = spaceService.update(10L, dto, "owner@test.com");

        assertThat(result.name()).isEqualTo("New Name");
    }

    // (d) update_asOwner_otherSpace_throwsAuthorizationException
    @Test
    void update_asOwner_otherSpace_throwsAuthorizationException() {
        User owner = buildUser(1L, "owner@test.com", Role.OWNER);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));

        Space space = buildSpace(10L, 99L, "Other Owner Space");
        when(spaceRepository.findById(10L)).thenReturn(Optional.of(space));

        SpaceUpdateRequestDto dto = new SpaceUpdateRequestDto(
                "Hacked Name", null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> spaceService.update(10L, dto, "owner@test.com"))
                .isInstanceOf(AuthorizationException.class);
    }

    // (e) delete_asOwner_ownSpace_succeeds
    @Test
    void delete_asOwner_ownSpace_succeeds() {
        User owner = buildUser(1L, "owner@test.com", Role.OWNER);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));

        Space space = buildSpace(10L, 1L, "My Space");
        when(spaceRepository.findById(10L)).thenReturn(Optional.of(space));

        assertThatCode(() -> spaceService.delete(10L, "owner@test.com"))
                .doesNotThrowAnyException();
    }

    // (f) delete_asAdmin_anySpace_succeeds
    @Test
    void delete_asAdmin_anySpace_succeeds() {
        User admin = buildUser(99L, "admin@test.com", Role.ADMIN);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        Space space = buildSpace(10L, 1L, "Someone Else Space");
        when(spaceRepository.findById(10L)).thenReturn(Optional.of(space));

        assertThatCode(() -> spaceService.delete(10L, "admin@test.com"))
                .doesNotThrowAnyException();
    }

    // (g) delete_asOwner_otherSpace_throwsAuthorizationException
    @Test
    void delete_asOwner_otherSpace_throwsAuthorizationException() {
        User owner = buildUser(1L, "owner@test.com", Role.OWNER);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));

        Space space = buildSpace(10L, 99L, "Not My Space");
        when(spaceRepository.findById(10L)).thenReturn(Optional.of(space));

        assertThatThrownBy(() -> spaceService.delete(10L, "owner@test.com"))
                .isInstanceOf(AuthorizationException.class);
    }

    // (h) getById_notFound_throwsResourceNotFoundException
    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(spaceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spaceService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // (i) create_nameReflectsInDto
    @Test
    void create_nameReflectsInDto() {
        User owner = buildUser(1L, "owner@test.com", Role.OWNER);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));

        Space saved = buildSpace(10L, 1L, "Test Space");
        when(spaceRepository.save(any(Space.class))).thenReturn(saved);

        SpaceDto result = spaceService.create(buildRequest("Test Space"), "owner@test.com");

        assertThat(result.name()).isEqualTo("Test Space");
    }
}
