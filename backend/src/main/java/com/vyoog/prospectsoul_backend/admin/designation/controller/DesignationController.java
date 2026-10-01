package com.vyoog.prospectsoul_backend.admin.designation.controller;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.admin.designation.dto.request.DesignationCreateRequest;
import com.vyoog.prospectsoul_backend.admin.designation.dto.request.DesignationUpdateRequest;
import com.vyoog.prospectsoul_backend.admin.designation.dto.response.DesignationResponse;
import com.vyoog.prospectsoul_backend.admin.designation.entity.DecisionMakerDesignation;
import com.vyoog.prospectsoul_backend.admin.designation.mapper.DesignationMapper;
import com.vyoog.prospectsoul_backend.admin.designation.repository.DecisionMakerDesignationRepository;
import com.vyoog.prospectsoul_backend.admin.designation.service.DecisionMakerDesignationService;
import com.vyoog.prospectsoul_backend.common.exception.ConflictException;
import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/decision-maker-designations")
@RequiredArgsConstructor
public class DesignationController {

    private final DecisionMakerDesignationRepository repository;
    private final DecisionMakerDesignationService service;
    private final DesignationMapper mapper;

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_READ)
    public List<DesignationResponse> list() {
        return service.listAll().stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.HAS_CONFIGURE)
    public DesignationResponse create(@Valid @RequestBody DesignationCreateRequest request) {
        if (repository.existsByDesignationIgnoreCase(request.designation())) {
            throw new ConflictException("Designation already exists: " + request.designation());
        }

        DecisionMakerDesignation entity = DecisionMakerDesignation.builder()
                .designation(request.designation().trim())
                .aliases(request.aliases() != null ? request.aliases() : List.of())
                .build();

        DecisionMakerDesignation saved = repository.save(entity);
        service.refreshCache();
        return mapper.toResponse(saved);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_CONFIGURE)
    public DesignationResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody DesignationUpdateRequest request) {
        DecisionMakerDesignation entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DecisionMakerDesignation", id));

        if (request.designation() != null) entity.setDesignation(request.designation().trim());
        if (request.aliases() != null) entity.setAliases(request.aliases());
        if (request.active() != null) entity.setActive(request.active());

        DecisionMakerDesignation saved = repository.save(entity);
        service.refreshCache();
        return mapper.toResponse(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(RoleConstants.HAS_CONFIGURE)
    public void deactivate(@PathVariable UUID id) {
        DecisionMakerDesignation entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DecisionMakerDesignation", id));
        entity.setActive(false);
        repository.save(entity);
        service.refreshCache();
    }
}
