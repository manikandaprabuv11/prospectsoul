package com.vyoog.prospectsoul_backend.company.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyCreateRequest(
        @NotBlank
        @Size(max = 500)
        String canonicalName,

        @Size(max = 500)
        String websiteDomain,

        @Size(max = 20)
        String primaryPhone,

        @Email
        @Size(max = 500)
        String email,

        @Size(max = 200)
        String city,

        @Size(max = 200)
        String state,

        @Size(max = 200)
        String cluster,

        @Size(max = 200)
        String industry,

        @Size(max = 50)
        String sizeBand,

        List<String> tags,

        @Size(max = 50)
        String source,

        // Sales-Intelligence extension.
        @Pattern(regexp = "^\\d{6}$", message = "pincode must be a 6-digit string")
        String pincode,

        @Size(max = 120) String district,
        String addressLine,
        @Size(max = 120) String region,
        String products,
        @Min(0) BigDecimal turnover,
        @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
                 message = "gst_number must match the 15-character GSTIN format")
        @Size(max = 15) String gstNumber,
        @Min(0) Integer employeeCount,
        LocalDate registrationDate,
        @Size(max = 120) String sourceReference,
        @Min(0) @Max(99) Short lgStateCode,
        @Min(0) Integer lgDistrictCode,
        UUID primaryNicCodeId,
        BigDecimal latitude,
        BigDecimal longitude
) {}
