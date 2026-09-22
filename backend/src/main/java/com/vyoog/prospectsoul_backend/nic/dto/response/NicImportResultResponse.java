package com.vyoog.prospectsoul_backend.nic.dto.response;

import java.util.List;

public record NicImportResultResponse(
        int rowsRead,
        int created,
        int updated,
        int unresolvedParents,
        int rejected,
        List<String> rejectedReasons
) {}
