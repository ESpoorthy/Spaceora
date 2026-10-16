package com.spaceora.auth.service;

import com.spaceora.auth.dto.AuthResponseDto;
import com.spaceora.auth.dto.LoginRequestDto;
import com.spaceora.auth.dto.RegisterRequestDto;
import com.spaceora.config.JwtProperties;
import com.spaceora.exception.AuthorizationException;
import com.spaceora.exception.DuplicateEmailException;
import com.spaceora.exception.ResourceNotFoundException;
import com.spaceora.user.dto.UpdateProfileRequestDto;
import com.spaceora.user.dto.UserProfileDto;
import com.spaceora.user.entity.Role;
import com.spaceora.user.entity.User;
import com.spaceora.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    public UserProfileDto register(RegisterRequestDto dto) {
        String email = dto.email().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        if (dto.role() == Role.ADMIN) {
            throw new AuthorizationException("Cannot self-assign ADMIN role");
        }

        // OWNER self-registration is intentionally permitted at the public endpoint.
        // See docs/api-contract.md: accepted role values at registration are CUSTOMER and OWNER.
        // ADMIN can only be granted by an existing ADMIN via PUT /api/admin/users/{id}/role.
        Role assignedRole = (dto.role() == null) ? Role.CUSTOMER : dto.role();

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(dto.password()))
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .role(assignedRole)
                .build();

        User saved = userRepository.save(user);
        return UserProfileDto.from(saved);
    }

    public AuthResponseDto login(LoginRequestDto dto) {
        String email = dto.email().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponseDto(token, jwtProperties.getExpirySeconds(), user.getId(), user.getRole().name());
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Authentication failed"));
    }

    public UserProfileDto getProfile(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserProfileDto.from(user);
    }

    public UserProfileDto updateProfile(String email, UpdateProfileRequestDto dto) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (dto.firstName() != null) {
            user.setFirstName(dto.firstName());
        }
        if (dto.lastName() != null) {
            user.setLastName(dto.lastName());
        }
        if (dto.phone() != null) {
            user.setPhone(dto.phone());
        }

        User saved = userRepository.save(user);
        return UserProfileDto.from(saved);
    }
}
