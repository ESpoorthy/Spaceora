package com.spaceora.space.service;

import com.spaceora.exception.AuthorizationException;
import com.spaceora.exception.ResourceNotFoundException;
import com.spaceora.space.dto.*;
import com.spaceora.space.entity.Space;
import com.spaceora.space.entity.SpaceType;
import com.spaceora.space.repository.SpaceImageRepository;
import com.spaceora.space.repository.SpaceRepository;
import com.spaceora.space.repository.SpaceSpecification;
import com.spaceora.user.entity.Role;
import com.spaceora.user.entity.User;
import com.spaceora.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceImageRepository spaceImageRepository;
    private final UserRepository userRepository;

    public SpaceService(SpaceRepository spaceRepository,
                        SpaceImageRepository spaceImageRepository,
                        UserRepository userRepository) {
        this.spaceRepository = spaceRepository;
        this.spaceImageRepository = spaceImageRepository;
        this.userRepository = userRepository;
    }

    public Page<SpaceSummaryDto> search(String city,
                                        SpaceType type,
                                        Integer minCap,
                                        Integer maxCap,
                                        BigDecimal minPrice,
                                        BigDecimal maxPrice,
                                        Pageable pageable) {
        Specification<Space> spec = SpaceSpecification.isActive();

        if (city != null && !city.isBlank()) {
            spec = spec.and(SpaceSpecification.hasCity(city));
        }
        if (type != null) {
            spec = spec.and(SpaceSpecification.hasType(type));
        }
        if (minCap != null) {
            spec = spec.and(SpaceSpecification.minCapacity(minCap));
        }
        if (maxCap != null) {
            spec = spec.and(SpaceSpecification.maxCapacity(maxCap));
        }
        if (minPrice != null) {
            spec = spec.and(SpaceSpecification.minPrice(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(SpaceSpecification.maxPrice(maxPrice));
        }

        return spaceRepository.findAll(spec, pageable).map(SpaceSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public SpaceDto getById(Long id) {
        Space space = spaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found: " + id));
        return SpaceDto.from(space);
    }

    public SpaceDto create(SpaceRequestDto dto, String ownerEmail) {
        User user = loadUserByEmail(ownerEmail);
        if (user.getRole() != Role.OWNER) {
            throw new AuthorizationException("Only owners can create spaces");
        }

        Space space = Space.builder()
                .ownerId(user.getId())
                .name(dto.name())
                .description(dto.description())
                .type(dto.type())
                .city(dto.city())
                .address(dto.address())
                .hourlyRate(dto.hourlyRate())
                .dailyRate(dto.dailyRate())
                .capacity(dto.capacity())
                .isActive(true)
                .build();

        Space saved = spaceRepository.save(space);
        return SpaceDto.from(saved);
    }

    public SpaceDto update(Long spaceId, SpaceUpdateRequestDto dto, String ownerEmail) {
        User user = loadUserByEmail(ownerEmail);
        Space space = loadSpaceById(spaceId);

        if (user.getRole() != Role.ADMIN && !space.getOwnerId().equals(user.getId())) {
            throw new AuthorizationException("Not your space");
        }

        if (dto.name() != null) {
            space.setName(dto.name());
        }
        if (dto.description() != null) {
            space.setDescription(dto.description());
        }
        if (dto.type() != null) {
            space.setType(dto.type());
        }
        if (dto.city() != null) {
            space.setCity(dto.city());
        }
        if (dto.address() != null) {
            space.setAddress(dto.address());
        }
        if (dto.hourlyRate() != null) {
            space.setHourlyRate(dto.hourlyRate());
        }
        if (dto.dailyRate() != null) {
            space.setDailyRate(dto.dailyRate());
        }
        if (dto.capacity() != null) {
            space.setCapacity(dto.capacity());
        }
        if (dto.isActive() != null) {
            space.setActive(dto.isActive());
        }

        Space saved = spaceRepository.save(space);
        return SpaceDto.from(saved);
    }

    public void delete(Long spaceId, String callerEmail) {
        User user = loadUserByEmail(callerEmail);
        Space space = loadSpaceById(spaceId);

        if (user.getRole() == Role.ADMIN) {
            spaceRepository.delete(space);
            return;
        }
        if (user.getRole() == Role.OWNER && space.getOwnerId().equals(user.getId())) {
            spaceRepository.delete(space);
            return;
        }
        throw new AuthorizationException("Not your space");
    }

    @Transactional(readOnly = true)
    public List<SpaceDto> getOwnerSpaces(String ownerEmail) {
        User user = loadUserByEmail(ownerEmail);
        return spaceRepository.findByOwnerId(user.getId())
                .stream()
                .map(SpaceDto::from)
                .toList();
    }

    // ---- helpers ----

    private User loadUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Space loadSpaceById(Long id) {
        return spaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found: " + id));
    }
}
