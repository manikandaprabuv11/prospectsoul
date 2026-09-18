package com.vyoog.prospectsoul_backend.verification.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.dto.PageResponse;
import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.verification.dto.request.StartVerificationRequest;
import com.vyoog.prospectsoul_backend.verification.dto.response.AddedByOptionResponse;
import com.vyoog.prospectsoul_backend.verification.dto.response.EligibleCompanyResponse;
import com.vyoog.prospectsoul_backend.verification.dto.response.VerificationBatchItemResponse;
import com.vyoog.prospectsoul_backend.verification.dto.response.VerificationBatchResponse;
import com.vyoog.prospectsoul_backend.verification.dto.response.VerifiedCompanyResponse;
import com.vyoog.prospectsoul_backend.verification.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Company phone verification (docs/dev_docs/14 §8).
 *
 * <p>Accepts and returns DTOs only; no JPA entity crosses this boundary.
 * Authorization is enforced here with {@code @PreAuthorize} — the frontend
 * hiding the Start button is UX, never the control.
 */
@RestController
@RequestMapping("/api/v1/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    /**
     * Starts a verification batch.
     *
     * <p>Returns {@code 202 Accepted}: the queue rows are committed, but no
     * company has been looked up yet. Progress is read back from
     * {@link #getActive()}, so the browser may close immediately.
     */
    @PostMapping
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public ResponseEntity<VerificationBatchResponse> start(@Valid @RequestBody StartVerificationRequest request,
                                                            Authentication auth) {
        VerificationBatchResponse batch = verificationService.start(request, CurrentUser.id(auth));
        return ResponseEntity.accepted().body(batch);
    }

    /**
     * The current non-terminal batch, or {@code 204 No Content} when nothing is
     * running. 204 rather than 404: "no batch is running" is a normal state of
     * the Verify page, not a missing resource.
     */
    @GetMapping("/active")
    @PreAuthorize(RoleConstants.HAS_READ)
    public ResponseEntity<VerificationBatchResponse> getActive() {
        return verificationService.getActive()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Previous verification jobs. */
    @GetMapping
    @PreAuthorize(RoleConstants.HAS_READ)
    public PageResponse<VerificationBatchResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(name = "requested_by", required = false) String requestedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir) {
        return verificationService.list(status, requestedBy, from, to, page, size, sort, sortDir);
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_READ)
    public VerificationBatchResponse getById(@PathVariable UUID id) {
        return verificationService.getById(id);
    }

    /** Per-company results for one job. */
    @GetMapping("/{id}/items")
    @PreAuthorize(RoleConstants.HAS_READ)
    public PageResponse<VerificationBatchItemResponse> getItems(
            @PathVariable UUID id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(name = "sort_dir", defaultValue = "asc") String sortDir) {
        return verificationService.getItems(id, status, q, page, size, sort, sortDir);
    }

    /**
     * Selection candidates for "Verify New Companies". Never returns a company
     * whose canonical status is already VERIFIED.
     */
    @GetMapping("/eligible")
    @PreAuthorize(RoleConstants.HAS_READ)
    public PageResponse<EligibleCompanyResponse> getEligible(
            @RequestParam(name = "added_by", required = false) String addedBy,
            @RequestParam(name = "date_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "date_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(name = "verification_status", required = false) String verificationStatus,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir) {
        return verificationService.getEligible(addedBy, dateFrom, dateTo, verificationStatus, q,
                page, size, sort, sortDir);
    }

    /** The verified-companies table. VERIFIED only. */
    @GetMapping("/companies")
    @PreAuthorize(RoleConstants.HAS_READ)
    public PageResponse<VerifiedCompanyResponse> getVerifiedCompanies(
            @RequestParam(required = false) String q,
            @RequestParam(name = "verified_by", required = false) String verifiedBy,
            @RequestParam(name = "verified_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate verifiedFrom,
            @RequestParam(name = "verified_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate verifiedTo,
            @RequestParam(name = "added_by", required = false) String addedBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "verifiedAt") String sort,
            @RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir) {
        return verificationService.getVerifiedCompanies(q, verifiedBy, verifiedFrom, verifiedTo,
                addedBy, page, size, sort, sortDir);
    }

    /**
     * Options for the Added By filter.
     *
     * <p>Addition to the seven endpoints named in docs/dev_docs/14 §8 and
     * docs/dev_docs/16: the "Added By: All / <user>" dropdown that
     * docs/dev_docs/17 §3 requires cannot be populated from any endpoint the
     * Analyst and Viewer roles can reach — the user directory
     * ({@code /api/v1/admin/users}) is Admin-only. This is additive and
     * read-only, and changes no existing contract.
     */
    @GetMapping("/added-by-options")
    @PreAuthorize(RoleConstants.HAS_READ)
    public List<AddedByOptionResponse> getAddedByOptions() {
        return verificationService.getAddedByOptions();
    }
}
