package com.tbm.careerpathlearning.controller;

import com.tbm.careerpathlearning.dto.OrgChartDto;
import com.tbm.careerpathlearning.service.OrgChartManagementService;
import com.tbm.careerpathlearning.service.OrgChartService;
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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orgChart")
public class OrgChartController {

    @Autowired
    private OrgChartService orgChartService;

    @Autowired
    private OrgChartManagementService orgChartManagementService;

    @Autowired
    private MessageSource messageSource;

    private static final String INTERNAL_SERVER_ERROR_EXP_ERR_TITLE_CODE = "internal.server.err.title";
    private static final String EXPORT_ERR_MSG_CODE = "export.data.err.msg";
    private static final String IMPORT_OK = "data.import.ok.msg";

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ACCESS_CONTROL.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_STAFF.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_PROPOSE_ROLE_COMPETENCIES.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ORG_CHART.getAuthorityName()
            )
            """)
    @GetMapping
    public ResponseEntity<?> getAllOrgChart(Authentication authentication) {
        return ResponseEntity.ok(orgChartService.findAllByIsDeletedIsFalse());
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).ROLE_USER.getAuthorityName()
            )
            """)
    @GetMapping("/show-org-chart")
    public ResponseEntity<?> showOrgChart(Authentication authentication) {
        return ResponseEntity.ok(orgChartManagementService.getOrgChartGraph());
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ORG_CHART.getAuthorityName()
            )
            """)
    @GetMapping("/export")
    public ResponseEntity<?> exportOrgChartData(Authentication authentication) {
        try {
            byte[] data = orgChartManagementService.exportData();
            ByteArrayResource resource = new ByteArrayResource(data);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Exported_Organizational_Chart_Data.xlsx");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (Exception e) {
            String errorTitle = messageSource.getMessage(INTERNAL_SERVER_ERROR_EXP_ERR_TITLE_CODE, null, Locale.getDefault());
            String errorMessage = messageSource.getMessage(EXPORT_ERR_MSG_CODE, null, Locale.getDefault());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("title", errorTitle, "message", errorMessage));
        }
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ORG_CHART.getAuthorityName()
            )
            """)
    @PostMapping("/import")
    public ResponseEntity<?> importOrgChartData(@RequestParam("file") MultipartFile file,
                                                Authentication authentication) throws Exception {
        orgChartManagementService.importData(file, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(IMPORT_OK, null, Locale.getDefault())
        ));
    }

    @GetMapping("/get-departments")
    public ResponseEntity<List<OrgChartDto>> getDepartmentList() {
        return ResponseEntity.ok(orgChartService.getDepartments());
    }
}
