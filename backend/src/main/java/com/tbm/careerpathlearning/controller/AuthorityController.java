package com.tbm.careerpathlearning.controller;

import com.tbm.careerpathlearning.dto.GrantAccessDto;
import com.tbm.careerpathlearning.service.AccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthorityController {

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private MessageSource messageSource;

    private static final String ACCESS_GRANTED_OK = "access.granted.ok.msg";
    private static final String ACCESS_REMOVED_OK = "access.remove.ok.msg";
    private static final String INTERNAL_SERVER_ERROR_EXP_ERR_TITLE_CODE = "internal.server.err.title";
    private static final String EXPORT_ERR_MSG_CODE = "export.data.err.msg";
    private static final String IMPORT_OK = "data.import.ok.msg";

    private static final String VIEW_OR_MANAGE_ACCESS_CONTROL = """
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_VIEW_ACCESS_CONTROL.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ACCESS_CONTROL.getAuthorityName()
            )
            """;

    private static final String MANAGE_ACCESS_CONTROL = """
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ACCESS_CONTROL.getAuthorityName()
            )
            """;

    @PreAuthorize(VIEW_OR_MANAGE_ACCESS_CONTROL)
    @GetMapping("/access-control-overview")
    public ResponseEntity<?> getStaffAccessControlOverview() {
        return ResponseEntity.ok(accessControlService.getOverview());
    }

    @PreAuthorize(MANAGE_ACCESS_CONTROL)
    @PostMapping("/grant-access")
    public ResponseEntity<?> grantAccess(@RequestBody GrantAccessDto grantAccessDto, Authentication auth) {
        accessControlService.grantAccess(grantAccessDto, UUID.fromString(auth.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(ACCESS_GRANTED_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(MANAGE_ACCESS_CONTROL)
    @PutMapping("/edit-granted-access")
    public ResponseEntity<?> editGrantedAccess(@RequestBody GrantAccessDto grantAccessDto, Authentication auth) {
        accessControlService.editGrantedAccess(grantAccessDto, UUID.fromString(auth.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(ACCESS_GRANTED_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(MANAGE_ACCESS_CONTROL)
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteRoleAuthority(@RequestParam Long orgChartId, @RequestParam Long roleId) {
        accessControlService.deleteRoleAuthority(orgChartId, roleId);
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(ACCESS_REMOVED_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(MANAGE_ACCESS_CONTROL)
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<?> bulkDeleteRoleAuthority(@RequestBody List<Long> selectedRoleIds) {
        accessControlService.bulkDeleteRoleAuthority(selectedRoleIds);
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(ACCESS_REMOVED_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(MANAGE_ACCESS_CONTROL)
    @GetMapping("/export")
    public ResponseEntity<?> exportRoleAssignmentData() {
        try {
            ByteArrayResource resource = accessControlService.exportData();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Exported_Access_Control_Data.xlsx");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            return ResponseEntity.ok().headers(headers).contentLength(resource.contentLength()).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "title", messageSource.getMessage(INTERNAL_SERVER_ERROR_EXP_ERR_TITLE_CODE, null, Locale.getDefault()),
                    "message", messageSource.getMessage(EXPORT_ERR_MSG_CODE, null, Locale.getDefault())
            ));
        }
    }

    @PreAuthorize(MANAGE_ACCESS_CONTROL)
    @PostMapping("/import")
    public ResponseEntity<?> importData(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        accessControlService.importData(file, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(IMPORT_OK, null, Locale.getDefault())));
    }
}
