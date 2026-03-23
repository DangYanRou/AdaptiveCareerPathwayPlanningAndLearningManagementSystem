package com.tbm.careerpathlearning.controller;

import com.tbm.careerpathlearning.dto.CreateRoleCompetencyRequestDto;
import com.tbm.careerpathlearning.dto.RoleCompetencyDto;
import com.tbm.careerpathlearning.service.RoleCompetencyManagementService;
import com.tbm.careerpathlearning.service.RoleCompetencyService;
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
@RequestMapping("/api/role-competency")
public class RoleCompetenciesController {

    @Autowired
    private RoleCompetencyService roleCompetencyService;

    @Autowired
    private RoleCompetencyManagementService roleCompetencyManagementService;

    @Autowired
    private MessageSource messageSource;

    private static final String COMPETENCY_ASSIGNMENT_CREATION_OK = "competency.assignment.creation.ok.msg";
    private static final String COMPETENCY_ASSIGNMENT_DELETE_OK = "competency.assignment.delete.ok.msg";
    private static final String INTERNAL_SERVER_ERROR_EXP_ERR_TITLE_CODE = "internal.server.err.title";
    private static final String EXPORT_ERR_MSG_CODE = "export.data.err.msg";
    private static final String IMPORT_OK = "data.import.ok.msg";

    @GetMapping
    public ResponseEntity<List<RoleCompetencyDto>> getAllRoleCompetencies() {
        return ResponseEntity.ok(roleCompetencyService.findAll());
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """)
    @GetMapping("/overview")
    public ResponseEntity<?> getCompetencyAssignmentOverview(Authentication authentication) {
        return ResponseEntity.ok(roleCompetencyManagementService.getOverview());
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).ROLE_USER.getAuthorityName()
            )
            """)
    @GetMapping("/role-details")
    public ResponseEntity<?> getRoleDetails(@RequestParam Long roleId, Authentication authentication) {
        return ResponseEntity.ok(roleCompetencyManagementService.getRoleDetails(roleId));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).ROLE_USER.getAuthorityName()
            )
            """)
    @GetMapping("/role-details-overview")
    public ResponseEntity<?> getRoleDetailsOverview(Authentication authentication) {
        return ResponseEntity.ok(roleCompetencyManagementService.getRoleDetailsOverview());
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """)
    @PostMapping
    public ResponseEntity<?> createRoleCompetency(@RequestBody CreateRoleCompetencyRequestDto requestDto,
                                                  Authentication authentication) {
        roleCompetencyManagementService.createRoleCompetency(requestDto, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(COMPETENCY_ASSIGNMENT_CREATION_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """)
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteCompetency(@RequestParam Long roleId, Authentication authentication) {
        roleCompetencyManagementService.deleteRoleCompetency(roleId);
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(COMPETENCY_ASSIGNMENT_DELETE_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """)
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<?> bulkDeleteCompetency(@RequestParam List<Long> roleIds, Authentication authentication) {
        roleCompetencyManagementService.bulkDeleteRoleCompetency(roleIds);
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(COMPETENCY_ASSIGNMENT_DELETE_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """)
    @GetMapping("/export")
    public ResponseEntity<?> exportCompetencyAssignmentData(Authentication authentication) {
        try {
            byte[] data = roleCompetencyManagementService.exportData();
            ByteArrayResource resource = new ByteArrayResource(data);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Exported_Competency_Assignment_Data.xlsx");
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
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """)
    @PostMapping("/import")
    public ResponseEntity<?> importData(@RequestParam("file") MultipartFile file,
                                        Authentication authentication) throws Exception {
        roleCompetencyManagementService.importData(file, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(IMPORT_OK, null, Locale.getDefault())
        ));
    }
}
