package com.vyoog.prospectsoul_backend.imports.controller;

import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.dto.PageResponse;
import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.imports.dto.request.ColumnMappingRequest;
import com.vyoog.prospectsoul_backend.imports.dto.response.ImportBatchResponse;
import com.vyoog.prospectsoul_backend.imports.dto.response.ImportPreviewResponse;
import com.vyoog.prospectsoul_backend.imports.dto.response.ImportRowResponse;
import com.vyoog.prospectsoul_backend.imports.dto.response.MappingSuggestionResponse;
import com.vyoog.prospectsoul_backend.imports.service.ImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public ImportBatchResponse upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "EXCEL_CSV") String source,
                                       Authentication auth) {
        return importService.upload(file, source, CurrentUser.id(auth));
    }

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_READ)
    public PageResponse<ImportBatchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return importService.listBatches(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_READ)
    public ImportBatchResponse getById(@PathVariable UUID id) {
        return importService.getBatch(id);
    }

    @GetMapping("/{id}/rows")
    @PreAuthorize(RoleConstants.HAS_READ)
    public PageResponse<ImportRowResponse> listRows(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return importService.listRows(id, page, size);
    }

    @GetMapping("/{id}/mappings/suggest")
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public MappingSuggestionResponse suggestMappings(@PathVariable UUID id) {
        return importService.detectAndSuggestMappings(id);
    }

    @PostMapping("/{id}/mappings/confirm")
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public ImportBatchResponse confirmMappings(@PathVariable UUID id,
                                                @Valid @RequestBody ColumnMappingRequest request,
                                                Authentication auth) {
        return importService.confirmMappings(id, request, CurrentUser.id(auth));
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public ImportPreviewResponse preview(@PathVariable UUID id) {
        return importService.preview(id);
    }

    @PostMapping("/{id}/process")
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public ImportBatchResponse startProcessing(@PathVariable UUID id,
                                                Authentication auth) {
        return importService.startProcessing(id, CurrentUser.id(auth));
    }
}
