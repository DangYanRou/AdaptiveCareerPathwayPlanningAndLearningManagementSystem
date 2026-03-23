package com.tbm.careerpathlearning.service;

import com.tbm.careerpathlearning.dto.RoleAssignmentOverviewDto;
import com.tbm.careerpathlearning.dto.UpdateRoleAssignmentRequestDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface RoleAssignmentManagementService {
    List<RoleAssignmentOverviewDto> getOverview();
    void createRoleAssignment(UpdateRoleAssignmentRequestDto requestDto, UUID userId);
    void updateRoleAssignment(UpdateRoleAssignmentRequestDto requestDto, UUID userId);
    void deleteRoleAssignment(Long roleId, UUID userId);
    void bulkDeleteRoleAssignment(List<Long> roleIds, UUID userId);
    byte[] exportData() throws IOException;
    void importData(MultipartFile file, UUID userId) throws IOException;
}
