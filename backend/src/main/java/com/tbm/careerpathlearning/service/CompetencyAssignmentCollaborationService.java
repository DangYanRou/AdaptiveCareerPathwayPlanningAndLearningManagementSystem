package com.tbm.careerpathlearning.service;

import com.tbm.careerpathlearning.dto.*;
import com.tbm.careerpathlearning.model.*;

import java.util.*;

public interface CompetencyAssignmentCollaborationService {

     List<CompetencyAssignmentCreationRequiredDataDto> getCreationRequiredCompetency(
            Long proposalId, Long roleId, UUID staffId, UUID userUUID) throws Exception; 

     void proposeCompetencyAssignment(ProposeCompetencyAssignmentRequest req, UUID userUUID) throws Exception; 

     List<RoleCompetencyProposalOverviewDto> getOverview(UUID userUUID) throws Exception;
    
     void updateCompetencyProposal(EditCompetencyAssignmentProposalRequestDto req, UUID userUUID) throws Exception;

     void rejectCompetencyAssignmentProposal(RoleCompetencyProposalId selectId, UUID userUUID) throws Exception; 

     void bulkRejectCompetencyProposal(Set<RoleCompetencyProposalId> selectedIds, UUID userUUID) throws Exception;
    
     void approveCompetencyAssignmentProposal(RoleCompetencyProposalId selectedId, UUID userUUID) throws Exception;

     void bulkApproveCompetencyProposal(Set<RoleCompetencyProposalId> selectedIds, UUID userUUID) throws Exception;
}
