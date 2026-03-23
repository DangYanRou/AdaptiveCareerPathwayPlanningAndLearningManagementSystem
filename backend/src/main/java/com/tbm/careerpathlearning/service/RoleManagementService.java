package com.tbm.careerpathlearning.service;

import com.tbm.careerpathlearning.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface RoleManagementService {
    List<RoleOverviewDto> getOverview();
    List<RoleJobScopeMapDto> getRoleJobScopeMap();
    void toggleVisibility(ToggleRoleVisibilityRequest request, UUID userId);
    RoleDto createRole(CreateRoleRequestDto requestDto, UUID userId);
    RoleDto updateRole(EditRoleRequestDto requestDto, UUID userId);
    void deleteRole(Long roleId, UUID userId);
    void bulkDeleteRole(List<Long> roleIds, UUID userId);
    byte[] exportData() throws IOException;
    void importData(MultipartFile file, UUID userId) throws IOException;
}
