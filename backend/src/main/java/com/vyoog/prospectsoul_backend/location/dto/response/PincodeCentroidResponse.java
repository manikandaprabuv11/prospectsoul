package com.vyoog.prospectsoul_backend.location.dto.response;

import java.math.BigDecimal;

public record PincodeCentroidResponse(
        String pincode,
        String areaName,
        String district,
        String state,
        Centroid centroid
) {
    public record Centroid(BigDecimal lat, BigDecimal lng) {}
}
