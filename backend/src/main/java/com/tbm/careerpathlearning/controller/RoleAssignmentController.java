package com.tbm.careerpathlearning.controller;

import com.tbm.careerpathlearning.dto.UpdateRoleAssignmentRequestDto;
import com.tbm.careerpathlearning.service.RoleAssignmentManagementService;
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
@RequestMapping("/api/role/assignment")
public class RoleAssignmentController {

    @Autowired
    private RoleAssignmentManagementService roleAssignmentManagementService;

    @Autowired
    private MessageSource messageSource;

    private static final String ROLE_ASSIGNMENT_CREATE_OK = "role.assignment.creation.ok.msg";
    private static final String ROLE_ASSIGNMENT_EDIT_OK = "role.assignment.edit.ok.msg";
    private static final String ROLE_ASSIGNMENT_DELETE_OK = "role.assignment.delete.ok.msg";
    private static final String INTERNAL_SERVER_ERROR_EXP_ERR_TITLE_CODE = "internal.server.err.title";
    private static final String EXPORT_ERR_MSG_CODE = "export.data.err.msg";
    private static final String IMPORT_OK = "data.import.ok.msg";

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_STAFF.getAuthorityName()
            )
            """)
    @GetMapping("/overview")
    public ResponseEntity<?> getRoleAssignmentOverview(Authentication authentication) {
        return ResponseEntity.ok(roleAssignmentManagementService.getOverview());
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_STAFF.getAuthorityName()
            )
            """)
    @PostMapping
    public ResponseEntity<?> createRoleAssignment(@RequestBody UpdateRoleAssignmentRequestDto requestDto,
                                                  Authentication authentication) {
        roleAssignmentManagementService.createRoleAssignment(requestDto, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(ROLE_ASSIGNMENT_CREATE_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_STAFF.getAuthorityName()
            )
            """)
    @PutMapping("edit-role-assignment")
    public ResponseEntity<?> updateRoleAssignment(@RequestBody UpdateRoleAssignmentRequestDto requestDto,
                                                  Authentication authentication) {
        roleAssignmentManagementService.updateRoleAssignment(requestDto, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(ROLE_ASSIGNMENT_EDIT_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_STAFF.getAuthorityName()
            )
            """)
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteRole(@RequestParam Long roleId, Authentication authentication) {
        roleAssignmentManagementService.deleteRoleAssignment(roleId, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(ROLE_ASSIGNMENT_DELETE_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_STAFF.getAuthorityName()
            )
            """)
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<?> bulkDeleteRole(@RequestParam List<Long> roleIds, Authentication authentication) {
        roleAssignmentManagementService.bulkDeleteRoleAssignment(roleIds, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(ROLE_ASSIGNMENT_DELETE_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_STAFF.getAuthorityName()
            )
            """)
    @GetMapping("/export")
    public ResponseEntity<?> exportRoleAssignmentData(Authentication authentication) {
        try {
            byte[] data = roleAssignmentManagementService.exportData();
            ByteArrayResource resource = new ByteArrayResource(data);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Exported_Role_Assignment_Data.xlsx");
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
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_STAFF.getAuthorityName()
            )
            """)
    @PostMapping("/import")
    public ResponseEntity<?> importData(@RequestParam("file") MultipartFile file,
                                        Authentication authentication) throws Exception {
        roleAssignmentManagementService.importData(file, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(IMPORT_OK, null, Locale.getDefault())
        ));
    }
}
