package com.vyoog.prospectsoul_backend.imports.controller;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.imports.dto.response.ImportTemplateResponse;
import com.vyoog.prospectsoul_backend.imports.entity.ImportTemplate;
import com.vyoog.prospectsoul_backend.imports.entity.ImportTemplateMapping;
import com.vyoog.prospectsoul_backend.imports.mapper.ImportMapper;
import com.vyoog.prospectsoul_backend.imports.mapping.ColumnAliasRegistry;
import com.vyoog.prospectsoul_backend.imports.repository.ImportTemplateRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/import-templates")
@RequiredArgsConstructor
public class ImportTemplateController {

    private final ImportTemplateRepository templateRepository;
    private final ImportMapper importMapper;
    private final ColumnAliasRegistry aliasRegistry;

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_READ)
    public List<ImportTemplateResponse> list() {
        return templateRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(importMapper::toTemplateResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public ImportTemplateResponse create(@Valid @RequestBody CreateTemplateRequest request,
                                          Authentication auth) {
        ImportTemplate template = ImportTemplate.builder()
                .name(request.name())
                .source(request.source())
                .createdBy(CurrentUser.id(auth))
                .build();

        if (request.mappings() != null) {
            for (var mapping : request.mappings()) {
                ImportTemplateMapping m = ImportTemplateMapping.builder()
                        .template(template)
                        .sourceHeader(mapping.sourceHeader())
                        .targetField(mapping.targetField())
                        .build();
                template.getMappings().add(m);
            }
        }

        template = templateRepository.save(template);
        return importMapper.toTemplateResponse(template);
    }

    @GetMapping("/aliases")
    @PreAuthorize(RoleConstants.HAS_READ)
    public java.util.Map<String, List<String>> getAliases() {
        return aliasRegistry.getAllAliases();
    }

    record CreateTemplateRequest(
            @NotBlank String name,
            @NotBlank String source,
            List<MappingEntry> mappings
    ) {
        record MappingEntry(String sourceHeader, String targetField) {}
    }
}
