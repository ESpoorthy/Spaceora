package com.spaceora.auth.service;

import com.spaceora.auth.dto.AuthResponseDto;
import com.spaceora.auth.dto.LoginRequestDto;
import com.spaceora.auth.dto.RegisterRequestDto;
import com.spaceora.config.JwtProperties;
import com.spaceora.exception.AuthorizationException;
import com.spaceora.exception.DuplicateEmailException;
import com.spaceora.user.dto.UpdateProfileRequestDto;
import com.spaceora.user.dto.UserProfileDto;
import com.spaceora.user.entity.Role;
import com.spaceora.user.entity.User;
import com.spaceora.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    // Use a real JwtProperties instance to avoid Byte Buddy / Java 25 mocking issues
    private JwtProperties jwtProperties;

    private UserService userService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-at-least-256-bits-long-for-hmac-sha256-algorithm");
        jwtProperties.setExpirySeconds(3600L);
        userService = new UserService(userRepository, passwordEncoder, jwtService, jwtProperties);
    }

    // ---- register tests ----

    @Test
    void register_validCustomer_returnsProfile() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass1!")).thenReturn("hashed");
        User saved = User.builder()
                .id(1L).email("alice@example.com").passwordHash("hashed")
                .firstName("Alice").lastName("Smith").role(Role.CUSTOMER)
                .createdAt(Instant.now()).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        RegisterRequestDto dto = new RegisterRequestDto("Alice", "Smith", "Alice@Example.com", "SecurePass1!", Role.CUSTOMER);
        UserProfileDto result = userService.register(dto);

        assertThat(result.email()).isEqualTo("alice@example.com");
        assertThat(result.role()).isEqualTo("CUSTOMER");
        verify(passwordEncoder).encode("SecurePass1!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsDuplicateEmailException() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        RegisterRequestDto dto = new RegisterRequestDto("Alice", "Smith", "alice@example.com", "SecurePass1!", null);

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void register_nullRole_defaultsToCustomer() {
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        User saved = User.builder()
                .id(2L).email("bob@example.com").passwordHash("hashed")
                .firstName("Bob").lastName("Jones").role(Role.CUSTOMER)
                .createdAt(Instant.now()).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        RegisterRequestDto dto = new RegisterRequestDto("Bob", "Jones", "bob@example.com", "SecurePass1!", null);
        UserProfileDto result = userService.register(dto);

        assertThat(result.role()).isEqualTo("CUSTOMER");
    }

    @Test
    void register_ownerRole_allowed() {
        when(userRepository.existsByEmail("owner@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        User saved = User.builder()
                .id(3L).email("owner@example.com").passwordHash("hashed")
                .firstName("Own").lastName("Er").role(Role.OWNER)
                .createdAt(Instant.now()).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        RegisterRequestDto dto = new RegisterRequestDto("Own", "Er", "owner@example.com", "SecurePass1!", Role.OWNER);
        UserProfileDto result = userService.register(dto);

        assertThat(result.role()).isEqualTo("OWNER");
    }

    @Test
    void register_adminRole_throwsAuthorizationException() {
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);

        RegisterRequestDto dto = new RegisterRequestDto("Ad", "Min", "admin@example.com", "SecurePass1!", Role.ADMIN);

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("ADMIN");
    }

    // ---- login tests ----

    @Test
    void login_success_returnsToken() {
        User user = User.builder()
                .id(1L).email("alice@example.com").passwordHash("hashed")
                .firstName("Alice").lastName("Smith").role(Role.CUSTOMER)
                .createdAt(Instant.now()).build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecurePass1!", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        LoginRequestDto dto = new LoginRequestDto("alice@example.com", "SecurePass1!");
        AuthResponseDto result = userService.login(dto);

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo("CUSTOMER");
        assertThat(result.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void login_wrongPassword_throwsBadCredentialsException() {
        User user = User.builder()
                .id(1L).email("alice@example.com").passwordHash("hashed")
                .firstName("Alice").lastName("Smith").role(Role.CUSTOMER).build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1!", "hashed")).thenReturn(false);

        LoginRequestDto dto = new LoginRequestDto("alice@example.com", "WrongPass1!");

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_unknownEmail_throwsBadCredentialsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        LoginRequestDto dto = new LoginRequestDto("unknown@example.com", "SecurePass1!");

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ---- getProfile tests ----

    @Test
    void getProfile_existingUser_returnsProfile() {
        User user = User.builder()
                .id(1L).email("alice@example.com").passwordHash("hashed")
                .firstName("Alice").lastName("Smith").role(Role.CUSTOMER)
                .createdAt(Instant.now()).build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        UserProfileDto result = userService.getProfile("alice@example.com");

        assertThat(result.email()).isEqualTo("alice@example.com");
        assertThat(result.firstName()).isEqualTo("Alice");
    }

    // ---- updateProfile tests ----

    @Test
    void updateProfile_changesFields_returnsUpdatedProfile() {
        User user = User.builder()
                .id(1L).email("alice@example.com").passwordHash("hashed")
                .firstName("Alice").lastName("Smith").role(Role.CUSTOMER)
                .createdAt(Instant.now()).build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequestDto dto = new UpdateProfileRequestDto("UpdatedName", null, "+1234567890");
        UserProfileDto result = userService.updateProfile("alice@example.com", dto);

        assertThat(result.firstName()).isEqualTo("UpdatedName");
        assertThat(result.phone()).isEqualTo("+1234567890");
    }

    // ---- loadUserByUsername tests ----

    @Test
    void loadUserByUsername_existingEmail_returnsUser() {
        User user = User.builder()
                .id(1L).email("alice@example.com").passwordHash("hashed")
                .firstName("Alice").lastName("Smith").role(Role.CUSTOMER).build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        var result = userService.loadUserByUsername("alice@example.com");

        assertThat(result.getUsername()).isEqualTo("alice@example.com");
    }

    @Test
    void loadUserByUsername_unknownEmail_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
