package com.vyoog.prospectsoul_backend.nic.controller;

import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.nic.dto.response.NicImportResultResponse;
import com.vyoog.prospectsoul_backend.nic.service.NicImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/nic-codes")
@RequiredArgsConstructor
public class NicImportController {

    private final NicImportService nicImportService;

    @PostMapping("/import")
    @PreAuthorize(RoleConstants.HAS_CONFIGURE)
    public NicImportResultResponse importFile(@RequestParam("file") MultipartFile file,
                                              Authentication auth) {
        return nicImportService.importFile(file, CurrentUser.id(auth));
    }
}
