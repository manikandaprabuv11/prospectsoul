package com.vyoog.prospectsoul_backend.company.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.dto.PageResponse;
import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import com.vyoog.prospectsoul_backend.company.dto.request.CompanyCreateRequest;
import com.vyoog.prospectsoul_backend.company.dto.request.CompanyUpdateRequest;
import com.vyoog.prospectsoul_backend.company.dto.response.CompanyResponse;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.mapper.CompanyMapper;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.company.specification.CompanySpecification;
import com.vyoog.prospectsoul_backend.imports.normalization.CompanyNameNormalizer;
import com.vyoog.prospectsoul_backend.imports.normalization.PhoneNormalizer;
import com.vyoog.prospectsoul_backend.imports.normalization.WebsiteNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "canonicalName", "city", "state", "industry", "cluster",
            "pipelineState", "verificationStatus", "createdAt", "updatedAt",
            "completenessScore", "source"
    ));

    private static final Set<String> CORE_FIELDS = Set.of(
            "canonicalName", "primaryPhoneNormalized", "websiteDomain", "email",
            "city", "state", "industry"
    );

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final AuditService auditService;
    private final CompanyNameNormalizer nameNormalizer;
    private final PhoneNormalizer phoneNormalizer;
    private final WebsiteNormalizer websiteNormalizer;

    @Transactional
    public CompanyResponse create(CompanyCreateRequest request, String actor) {
        Company company = Company.builder()
                .canonicalName(request.canonicalName().trim())
                .normalizedName(nameNormalizer.normalize(request.canonicalName()))
                .websiteDomain(normalizeWebsite(request.websiteDomain()))
                .primaryPhoneNormalized(normalizePhone(request.primaryPhone()))
                .email(trimOrNull(request.email()))
                .city(trimOrNull(request.city()))
                .state(trimOrNull(request.state()))
                .cluster(trimOrNull(request.cluster()))
                .industry(trimOrNull(request.industry()))
                .sizeBand(trimOrNull(request.sizeBand()))
                .tags(request.tags())
                .source(request.source() != null ? request.source() : "MANUAL_ENTRY")
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        company.setCompletenessScore(computeCompleteness(company));
        Company saved = companyRepository.save(company);

        auditService.record("COMPANY", saved.getId(), actor, "CREATE", null, companyMapper.toResponse(saved));
        return companyMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
        return companyMapper.toResponse(company);
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> list(String search, String city, String state,
                                               String industry, String cluster, String source,
                                               String pipelineState, String verificationStatus,
                                               int page, int size, String sortField, String sortDir) {
        size = Math.min(size, MAX_PAGE_SIZE);
        String resolvedSortField = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSortField));

        Page<Company> result = companyRepository.findAll(
                CompanySpecification.withFilters(search, city, state, industry, cluster,
                        source, pipelineState, verificationStatus),
                pageable
        );

        return PageResponse.from(result.map(companyMapper::toResponse));
    }

    @Transactional
    public CompanyResponse update(UUID id, CompanyUpdateRequest request, String actor) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));

        CompanyResponse previousState = companyMapper.toResponse(company);
        boolean coreFieldChanged = false;

        if (request.canonicalName() != null) {
            company.setCanonicalName(request.canonicalName().trim());
            company.setNormalizedName(nameNormalizer.normalize(request.canonicalName()));
            coreFieldChanged = true;
        }
        if (request.websiteDomain() != null) {
            company.setWebsiteDomain(normalizeWebsite(request.websiteDomain()));
            coreFieldChanged = true;
        }
        if (request.primaryPhone() != null) {
            company.setPrimaryPhoneNormalized(normalizePhone(request.primaryPhone()));
            coreFieldChanged = true;
        }
        if (request.email() != null) {
            company.setEmail(trimOrNull(request.email()));
            coreFieldChanged = true;
        }
        if (request.city() != null) {
            company.setCity(trimOrNull(request.city()));
            coreFieldChanged = true;
        }
        if (request.state() != null) {
            company.setState(trimOrNull(request.state()));
            coreFieldChanged = true;
        }
        if (request.cluster() != null) {
            company.setCluster(trimOrNull(request.cluster()));
        }
        if (request.industry() != null) {
            company.setIndustry(trimOrNull(request.industry()));
            coreFieldChanged = true;
        }
        if (request.sizeBand() != null) {
            company.setSizeBand(trimOrNull(request.sizeBand()));
        }
        if (request.tags() != null) {
            company.setTags(request.tags());
        }
        if (request.source() != null) {
            company.setSource(request.source());
        }

        if (coreFieldChanged && company.getVerificationStatus() == Company.VerificationStatus.VERIFIED) {
            company.setVerificationStatus(Company.VerificationStatus.INVALIDATED);
            company.setVerifiedBy(null);
            company.setVerifiedAt(null);
        }

        company.setUpdatedBy(actor);
        company.setCompletenessScore(computeCompleteness(company));
        Company saved = companyRepository.save(company);

        auditService.record("COMPANY", saved.getId(), actor, "UPDATE",
                previousState, companyMapper.toResponse(saved));
        return companyMapper.toResponse(saved);
    }

    @Transactional
    public CompanyResponse verify(UUID id, String actor) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));

        CompanyResponse previousState = companyMapper.toResponse(company);

        company.setVerificationStatus(Company.VerificationStatus.VERIFIED);
        company.setVerifiedBy(actor);
        company.setVerifiedAt(Instant.now());
        company.setUpdatedBy(actor);
        Company saved = companyRepository.save(company);

        auditService.record("COMPANY", saved.getId(), actor, "VERIFY",
                previousState, companyMapper.toResponse(saved));
        return companyMapper.toResponse(saved);
    }

    public List<CompanyResponse> findDuplicates(String normalizedPhone, String websiteDomain,
                                                 String normalizedName, String city) {
        if (normalizedPhone != null && !normalizedPhone.isBlank()) {
            return companyRepository.findByPrimaryPhoneNormalized(normalizedPhone)
                    .map(c -> List.of(companyMapper.toResponse(c)))
                    .orElse(List.of());
        }
        if (websiteDomain != null && !websiteDomain.isBlank()) {
            return companyRepository.findByWebsiteDomain(websiteDomain)
                    .map(c -> List.of(companyMapper.toResponse(c)))
                    .orElse(List.of());
        }
        if (normalizedName != null && !normalizedName.isBlank() && city != null && !city.isBlank()) {
            return companyRepository.findByNormalizedNameAndCity(normalizedName, city)
                    .map(c -> List.of(companyMapper.toResponse(c)))
                    .orElse(List.of());
        }
        return List.of();
    }

    private int computeCompleteness(Company company) {
        int score = 0;
        int total = 8;
        if (company.getCanonicalName() != null && !company.getCanonicalName().isBlank()) score++;
        if (company.getPrimaryPhoneNormalized() != null && !company.getPrimaryPhoneNormalized().isBlank()) score++;
        if (company.getEmail() != null && !company.getEmail().isBlank()) score++;
        if (company.getWebsiteDomain() != null && !company.getWebsiteDomain().isBlank()) score++;
        if (company.getCity() != null && !company.getCity().isBlank()) score++;
        if (company.getState() != null && !company.getState().isBlank()) score++;
        if (company.getIndustry() != null && !company.getIndustry().isBlank()) score++;
        if (company.getSource() != null && !company.getSource().isBlank()) score++;
        return (int) Math.round((score * 100.0) / total);
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        return phoneNormalizer.normalize(phone);
    }

    private String normalizeWebsite(String website) {
        if (website == null || website.isBlank()) return null;
        return websiteNormalizer.normalize(website);
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
