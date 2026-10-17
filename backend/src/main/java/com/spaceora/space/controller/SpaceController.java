package com.spaceora.space.controller;

import com.spaceora.space.dto.SpaceDto;
import com.spaceora.space.dto.SpaceRequestDto;
import com.spaceora.space.dto.SpaceSummaryDto;
import com.spaceora.space.dto.SpaceUpdateRequestDto;
import com.spaceora.space.entity.SpaceType;
import com.spaceora.space.service.SpaceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "name", "hourlyRate", "capacity", "createdAt");

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @GetMapping
    public ResponseEntity<?> searchSpaces(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) Integer maxCapacity,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        // Cap size at 100
        int cappedSize = Math.min(size, 100);

        // Parse type string to SpaceType
        SpaceType spaceType = null;
        if (type != null && !type.isBlank()) {
            try {
                spaceType = SpaceType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid space type: " + type);
            }
        }

        // Parse sort as 'field,direction'; validate field against whitelist
        Sort parsedSort;
        String[] sortParts = sort.split(",", 2);
        String sortField = sortParts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            return ResponseEntity.badRequest().body("Invalid sort field: " + sortField
                    + ". Allowed: " + ALLOWED_SORT_FIELDS);
        }
        Sort.Direction sortDirection = (sortParts.length > 1 && sortParts[1].trim().equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        parsedSort = Sort.by(sortDirection, sortField);

        Pageable pageable = PageRequest.of(page, cappedSize, parsedSort);
        Page<SpaceSummaryDto> result = spaceService.search(city, spaceType, minCapacity, maxCapacity, minPrice, maxPrice, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/owner/me")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<SpaceDto>> getOwnerSpaces(Authentication auth) {
        return ResponseEntity.ok(spaceService.getOwnerSpaces(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpaceDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(spaceService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<SpaceDto> createSpace(
            @Valid @RequestBody SpaceRequestDto dto,
            Authentication auth) {
        SpaceDto created = spaceService.create(dto, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<SpaceDto> updateSpace(
            @PathVariable Long id,
            @Valid @RequestBody SpaceUpdateRequestDto dto,
            Authentication auth) {
        return ResponseEntity.ok(spaceService.update(id, dto, auth.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSpace(
            @PathVariable Long id,
            Authentication auth) {
        spaceService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
