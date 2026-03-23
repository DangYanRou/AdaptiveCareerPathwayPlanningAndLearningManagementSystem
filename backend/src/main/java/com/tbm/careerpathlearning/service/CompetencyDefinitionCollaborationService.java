package com.tbm.careerpathlearning.service;

import com.tbm.careerpathlearning.dto.CompetencyProposalOverviewDto;
import com.tbm.careerpathlearning.dto.EditCompetencyProposalRequestDto;
import com.tbm.careerpathlearning.dto.ProposeCompetencyRequest;
import com.tbm.careerpathlearning.model.ProposalParticipantId;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CompetencyDefinitionCollaborationService {
    List<CompetencyProposalOverviewDto> getOverview(UUID userId);
    void proposeCompetency(ProposeCompetencyRequest req, UUID userId, List<String> userRoles);
    void updateCompetencyProposal(EditCompetencyProposalRequestDto req, UUID userId);
    void rejectCompetencyProposal(ProposalParticipantId selectedId, UUID userId);
    void bulkRejectCompetencyProposal(Set<ProposalParticipantId> selectedIds, UUID userId);
    void approveCompetencyProposal(ProposalParticipantId selectedId, UUID userId);
    void bulkApproveCompetencyProposal(Set<ProposalParticipantId> selectedIds, UUID userId);
}
