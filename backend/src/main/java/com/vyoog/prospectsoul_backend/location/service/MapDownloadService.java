package com.vyoog.prospectsoul_backend.location.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.nic.entity.CompanyNicCode;
import com.vyoog.prospectsoul_backend.company.nic.repository.CompanyNicCodeRepository;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.location.entity.PincodeCentroid;
import com.vyoog.prospectsoul_backend.location.resolution.PincodeResolutionService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MapDownloadService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Kolkata"));

    private static final String[] HEADERS = {
            "S.No", "Company Name", "Pincode", "NIC Codes", "Full Address",
            "City", "State", "District", "Phone", "Email", "Website",
            "Pipeline State", "Latitude", "Longitude", "Date Created"
    };

    private final CompanyRepository companyRepository;
    private final CompanyNicCodeRepository companyNicCodeRepository;
    private final PincodeResolutionService pincodeResolver;
    private final AuditService auditService;

    public record DownloadResult(byte[] bytes, String fileName) {}

    @Transactional
    public DownloadResult download(String pincode, double radiusKm,
                                   Collection<UUID> nicCodeIds, String actor) {

        Set<UUID> nicCompanyIds = nicCodeIds == null
                ? null
                : (nicCodeIds.isEmpty()
                        ? Set.of()
                        : Set.copyOf(companyNicCodeRepository.findCompanyIdsByNicCodeIdIn(nicCodeIds)));

        BigDecimal centreLat;
        BigDecimal centreLng;
        try {
            PincodeCentroid master = pincodeResolver.resolve(pincode);
            centreLat = master.getLatitude();
            centreLng = master.getLongitude();
        } catch (RuntimeException e) {
            var pincodeMatches = companyRepository.findAll().stream()
                    .filter(c -> pincode.equals(c.getPincode()))
                    .filter(this::hasCoords)
                    .toList();
            if (!pincodeMatches.isEmpty()) {
                centreLat = pincodeMatches.stream().map(this::effectiveLat)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(pincodeMatches.size()), java.math.RoundingMode.HALF_UP);
                centreLng = pincodeMatches.stream().map(this::effectiveLng)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(pincodeMatches.size()), java.math.RoundingMode.HALF_UP);
            } else {
                centreLat = new BigDecimal("20.593700");
                centreLng = new BigDecimal("78.962900");
            }
        }

        List<Company> pincodeMatches = companyRepository.findAll().stream()
                .filter(c -> pincode.equals(c.getPincode()))
                .filter(c -> nicCompanyIds == null || nicCompanyIds.contains(c.getId()))
                .toList();

        final BigDecimal cLat = centreLat;
        final BigDecimal cLng = centreLng;
        List<Company> radiusExtras = companyRepository.findAll().stream()
                .filter(c -> !pincode.equals(c.getPincode()))
                .filter(this::hasCoords)
                .filter(c -> haversineKm(cLat, cLng, effectiveLat(c), effectiveLng(c)) <= radiusKm)
                .filter(c -> nicCompanyIds == null || nicCompanyIds.contains(c.getId()))
                .toList();

        LinkedHashMap<UUID, Company> combined = new LinkedHashMap<>();
        pincodeMatches.forEach(c -> combined.put(c.getId(), c));
        radiusExtras.forEach(c -> combined.putIfAbsent(c.getId(), c));

        List<Company> companies = new ArrayList<>(combined.values());

        Map<UUID, String> nicCodesMap = buildNicCodesMap(List.copyOf(combined.keySet()));

        byte[] bytes;
        try {
            bytes = writeXlsx(companies, nicCodesMap, cLat, cLng);
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to produce download: " + e.getMessage());
        }

        auditService.record("MAP_DOWNLOAD", UUID.randomUUID(), actor, "DOWNLOAD",
                null, Map.of(
                        "pincode", pincode,
                        "radius_km", radiusKm,
                        "row_count", companies.size(),
                        "nic_filter_active", nicCodeIds != null));

        String fileName = "map-companies-" + pincode + "-" + (int) radiusKm + "km.xlsx";
        return new DownloadResult(bytes, fileName);
    }

    private Map<UUID, String> buildNicCodesMap(List<UUID> companyIds) {
        if (companyIds.isEmpty()) return Map.of();
        Map<UUID, List<String>> grouped = new LinkedHashMap<>();
        for (CompanyNicCode cnc : companyNicCodeRepository
                .findByCompanyIdInOrderByCompanyIdAscSequenceNoAsc(companyIds)) {
            String code = cnc.getNicCode() != null ? cnc.getNicCode().getCode() : cnc.getNicCodeRaw();
            if (code != null) {
                grouped.computeIfAbsent(cnc.getCompanyId(), k -> new ArrayList<>()).add(code);
            }
        }
        return grouped.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.join(", ", e.getValue())));
    }

    private byte[] writeXlsx(List<Company> companies, Map<UUID, String> nicCodesMap,
                             BigDecimal fallbackLat, BigDecimal fallbackLng) throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("Map Companies");

            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                var cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int r = 1;
            for (Company c : companies) {
                Row row = sheet.createRow(r);
                int col = 0;
                row.createCell(col++).setCellValue(r);
                row.createCell(col++).setCellValue(safe(c.getCanonicalName()));
                row.createCell(col++).setCellValue(safe(c.getPincode()));
                row.createCell(col++).setCellValue(safe(nicCodesMap.get(c.getId())));
                row.createCell(col++).setCellValue(buildAddress(c));
                row.createCell(col++).setCellValue(safe(c.getCity()));
                row.createCell(col++).setCellValue(safe(c.getState()));
                row.createCell(col++).setCellValue(safe(c.getDistrict()));
                row.createCell(col++).setCellValue(safe(c.getPrimaryPhoneNormalized()));
                row.createCell(col++).setCellValue(safe(c.getEmail()));
                row.createCell(col++).setCellValue(safe(c.getWebsiteDomain()));
                row.createCell(col++).setCellValue(c.getPipelineState().name());

                BigDecimal lat = effectiveLat(c) != null ? effectiveLat(c) : fallbackLat;
                BigDecimal lng = effectiveLng(c) != null ? effectiveLng(c) : fallbackLng;
                row.createCell(col++).setCellValue(lat != null ? lat.doubleValue() : 0);
                row.createCell(col++).setCellValue(lng != null ? lng.doubleValue() : 0);

                row.createCell(col).setCellValue(
                        c.getCreatedAt() != null ? DATE_FMT.format(c.getCreatedAt()) : "");
                r++;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            wb.dispose();
            return out.toByteArray();
        }
    }

    private String buildAddress(Company c) {
        StringBuilder sb = new StringBuilder();
        if (c.getAddressLine() != null && !c.getAddressLine().isBlank()) sb.append(c.getAddressLine());
        if (c.getCity() != null && !c.getCity().isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(c.getCity());
        }
        if (c.getDistrict() != null && !c.getDistrict().isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(c.getDistrict());
        }
        if (c.getState() != null && !c.getState().isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(c.getState());
        }
        if (c.getPincode() != null && !c.getPincode().isBlank()) {
            if (!sb.isEmpty()) sb.append(" - ");
            sb.append(c.getPincode());
        }
        return sb.toString();
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private BigDecimal effectiveLat(Company c) {
        return c.getLatitude() != null ? c.getLatitude() : c.getGoogleLat();
    }

    private BigDecimal effectiveLng(Company c) {
        return c.getLongitude() != null ? c.getLongitude() : c.getGoogleLng();
    }

    private boolean hasCoords(Company c) {
        return effectiveLat(c) != null && effectiveLng(c) != null;
    }

    private double haversineKm(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        double R = 6371.0088;
        double dLat = Math.toRadians(lat2.subtract(lat1, MathContext.DECIMAL64).doubleValue());
        double dLng = Math.toRadians(lng2.subtract(lng1, MathContext.DECIMAL64).doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
