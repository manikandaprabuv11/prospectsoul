package com.vyoog.prospectsoul_backend.company.defaultfilter.service;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.exception.ConflictException;
import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import com.vyoog.prospectsoul_backend.company.defaultfilter.dto.request.CompanyDefaultFilterUpsertRequest;
import com.vyoog.prospectsoul_backend.company.defaultfilter.dto.response.CompanyDefaultFilterResponse;
import com.vyoog.prospectsoul_backend.company.defaultfilter.entity.CompanyDefaultFilter;
import com.vyoog.prospectsoul_backend.company.defaultfilter.mapper.CompanyDefaultFilterMapper;
import com.vyoog.prospectsoul_backend.company.defaultfilter.repository.CompanyDefaultFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyDefaultFilterService {

    private final CompanyDefaultFilterRepository repository;
    private final CompanyDefaultFilterMapper mapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CompanyDefaultFilterResponse> list(boolean includeInactive) {
        var rows = includeInactive
                ? repository.findAllByOrderBySortOrderAscLabelAsc()
                : repository.findByActiveTrueOrderBySortOrderAscLabelAsc();
        return rows.stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public CompanyDefaultFilterResponse create(CompanyDefaultFilterUpsertRequest req, String actor) {
        if (repository.findByFilterKey(req.filterKey()).isPresent()) {
            throw new ConflictException("Filter key already exists: " + req.filterKey());
        }
        CompanyDefaultFilter saved = repository.save(CompanyDefaultFilter.builder()
                .filterKey(req.filterKey().trim())
                .label(req.label().trim())
                .operator(req.operator())
                .value(req.value())
                .active(req.active() == null ? Boolean.TRUE : req.active())
                .sortOrder(req.sortOrder() == null ? (short) 100 : req.sortOrder())
                .createdBy(actor)
                .updatedBy(actor)
                .build());
        auditService.record("COMPANY_DEFAULT_FILTER", saved.getId(), actor, "CREATE",
                null, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public CompanyDefaultFilterResponse update(UUID id, CompanyDefaultFilterUpsertRequest req, String actor) {
        CompanyDefaultFilter row = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyDefaultFilter", id));
        var before = mapper.toResponse(row);
        row.setLabel(req.label().trim());
        row.setOperator(req.operator());
        row.setValue(req.value());
        if (req.active() != null) row.setActive(req.active());
        if (req.sortOrder() != null) row.setSortOrder(req.sortOrder());
        row.setUpdatedBy(actor);
        CompanyDefaultFilter saved = repository.save(row);
        auditService.record("COMPANY_DEFAULT_FILTER", saved.getId(), actor, "UPDATE",
                before, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id, String actor) {
        CompanyDefaultFilter row = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyDefaultFilter", id));
        var before = mapper.toResponse(row);
        repository.delete(row);
        auditService.record("COMPANY_DEFAULT_FILTER", id, actor, "DELETE", before, null);
    }

    /** Read-only accessor used by CompanyService when {@code apply_defaults=true}. */
    @Transactional(readOnly = true)
    public List<CompanyDefaultFilter> activeDefaults() {
        return repository.findByActiveTrueOrderBySortOrderAscLabelAsc();
    }
}
