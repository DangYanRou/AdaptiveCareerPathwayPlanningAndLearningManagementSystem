package com.tbm.careerpathlearning.service;

import com.tbm.careerpathlearning.dto.CompetencyAssignmentOverviewDto;
import com.tbm.careerpathlearning.dto.CreateRoleCompetencyRequestDto;
import com.tbm.careerpathlearning.dto.RoleDetailsDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface RoleCompetencyManagementService {
    List<CompetencyAssignmentOverviewDto> getOverview();
    RoleDetailsDto getRoleDetails(Long roleId);
    List<RoleDetailsDto> getRoleDetailsOverview();
    void createRoleCompetency(CreateRoleCompetencyRequestDto requestDto, UUID userId);
    void deleteRoleCompetency(Long roleId);
    void bulkDeleteRoleCompetency(List<Long> roleIds);
    byte[] exportData() throws IOException;
    void importData(MultipartFile file, UUID userId) throws IOException;
}
