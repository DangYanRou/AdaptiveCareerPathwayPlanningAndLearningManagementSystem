package com.tbm.careerpathlearning.controller;

import com.tbm.careerpathlearning.dto.*;
import com.tbm.careerpathlearning.service.RoleManagementService;
import com.tbm.careerpathlearning.service.RoleService;
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
@RequestMapping("/api/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleManagementService roleManagementService;

    @Autowired
    private MessageSource messageSource;

    private static final String TOGGLE_VISIBILITY_OK = "toggle.role.visibility.ok.msg";
    private static final String ROLE_CREATION_OK = "role.creation.ok.msg";
    private static final String ROLE_EDIT_OK = "role.edit.ok.msg";
    private static final String ROLE_DELETE_OK = "role.delete.ok.msg";
    private static final String INTERNAL_SERVER_ERROR_EXP_ERR_TITLE_CODE = "internal.server.err.title";
    private static final String EXPORT_ERR_MSG_CODE = "export.data.err.msg";
    private static final String IMPORT_OK = "data.import.ok.msg";

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ACCESS_CONTROL.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_STAFF.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_PROPOSE_ROLE_COMPETENCIES.getAuthorityName()
            )
            """)
    @GetMapping
    public ResponseEntity<?> getAllRoles(Authentication authentication) {
        return ResponseEntity.ok(roleService.getAllByDeletedIsFalse());
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).ROLE_USER.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_VIEW_INVISIBLE_ROLE.getAuthorityName()
            )
            """)
    @GetMapping("/overview")
    public ResponseEntity<?> getRoleOverview(Authentication authentication) {
        return ResponseEntity.ok(roleManagementService.getOverview());
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """)
    @GetMapping("/jobScope-map")
    public ResponseEntity<?> getAllRoleJobScopeMap() {
        return ResponseEntity.ok(roleManagementService.getRoleJobScopeMap());
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """)
    @PutMapping("/toggle-visibility")
    public ResponseEntity<?> toggleVisibility(@RequestBody ToggleRoleVisibilityRequest request, Authentication authentication) {
        roleManagementService.toggleVisibility(request, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(TOGGLE_VISIBILITY_OK, null, Locale.getDefault())));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """)
    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody CreateRoleRequestDto requestDto, Authentication authentication) {
        RoleDto createdRoleDto = roleManagementService.createRole(requestDto, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(ROLE_CREATION_OK, null, Locale.getDefault()),
                "createdRole", createdRoleDto
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """)
    @PutMapping("/edit-role")
    public ResponseEntity<?> updateRole(@RequestBody EditRoleRequestDto requestDto, Authentication authentication) {
        RoleDto updatedRoleDto = roleManagementService.updateRole(requestDto, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(ROLE_EDIT_OK, null, Locale.getDefault()),
                "updatedRole", updatedRoleDto
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """)
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteRole(@RequestParam Long roleId, Authentication authentication) {
        roleManagementService.deleteRole(roleId, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(ROLE_DELETE_OK, null, Locale.getDefault())));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """)
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<?> bulkDeleteRole(@RequestParam List<Long> roleIds, Authentication authentication) {
        roleManagementService.bulkDeleteRole(roleIds, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(ROLE_DELETE_OK, null, Locale.getDefault())));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """)
    @GetMapping("/export")
    public ResponseEntity<?> exportRoleOverviewData(Authentication authentication) {
        try {
            byte[] data = roleManagementService.exportData();
            ByteArrayResource resource = new ByteArrayResource(data);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Exported_Role_Overview_Data.xlsx");
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
        roleManagementService.importData(file, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(IMPORT_OK, null, Locale.getDefault())
        ));
    }
}
