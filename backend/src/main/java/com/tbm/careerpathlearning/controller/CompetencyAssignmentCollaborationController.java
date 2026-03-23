package com.tbm.careerpathlearning.controller;

import com.tbm.careerpathlearning.dto.EditCompetencyAssignmentProposalRequestDto;
import com.tbm.careerpathlearning.dto.ProposeCompetencyAssignmentRequest;
import com.tbm.careerpathlearning.model.RoleCompetencyProposalId;
import com.tbm.careerpathlearning.service.CompetencyAssignmentCollaborationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/competency-assignment-collaboration")
@CrossOrigin
public class CompetencyAssignmentCollaborationController {

    @Autowired
    private CompetencyAssignmentCollaborationService collaborationService;

    @Autowired
    private MessageSource messageSource;

    private static final String COMPETENCY_PROPOSAL_ASSIGNMENT_CREATION_OK = "proposal.competency.assignment.creation.ok.msg";
    private static final String COMPETENCY_PROPOSAL_ASSIGNMENT_EDIT_OK = "proposal.competency.assignment.edit.ok.msg";
    private static final String COMPETENCY_PROPOSAL_ASSIGNMENT_REJECT_OK = "proposal.competency.assignment.reject.ok.msg";
    private static final String COMPETENCY_PROPOSAL_ASSIGNMENT_APPROVE_OK = "proposal.competency.assignment.approve.ok.msg";

    private static final String MANAGE_OR_PROPOSE = """
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_PROPOSE_ROLE_COMPETENCIES.getAuthorityName()
            )
            """;

    private static final String MANAGE_ROLE = """
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName()
            )
            """;

    @PreAuthorize(MANAGE_OR_PROPOSE)
    @GetMapping("/creation-required-competency")
    public ResponseEntity<?> getCompetencyForProposingCompetencyAssignment(
            @RequestParam(required = false) Long proposalId,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) UUID staffId,
            Authentication authentication) throws Exception {
        UUID userUUID = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(collaborationService.getCreationRequiredCompetency(proposalId, roleId, staffId, userUUID));
    }

    @PreAuthorize(MANAGE_ROLE)
    @PostMapping
    public ResponseEntity<?> proposeCompetencyAssignment(@RequestBody ProposeCompetencyAssignmentRequest req,
                                                         Authentication authentication) throws Exception {
        collaborationService.proposeCompetencyAssignment(req, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(COMPETENCY_PROPOSAL_ASSIGNMENT_CREATION_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(MANAGE_OR_PROPOSE)
    @GetMapping("/overview")
    public ResponseEntity<?> competencyAssignmentProposalOverview(Authentication authentication) throws Exception {
        return ResponseEntity.ok(collaborationService.getOverview(UUID.fromString(authentication.getName())));
    }

    @PreAuthorize(MANAGE_OR_PROPOSE)
    @PutMapping("/edit-competency-assignment-proposal")
    public ResponseEntity<?> updateCompetencyProposal(@RequestBody EditCompetencyAssignmentProposalRequestDto req,
                                                      Authentication authentication) throws Exception {
        collaborationService.updateCompetencyProposal(req, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(COMPETENCY_PROPOSAL_ASSIGNMENT_EDIT_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(MANAGE_ROLE)
    @PutMapping("/reject-competency-assignment-proposal")
    public ResponseEntity<?> rejectCompetencyAssignmentProposal(@RequestBody RoleCompetencyProposalId selectId,
                                                                 Authentication authentication) throws Exception {
        collaborationService.rejectCompetencyAssignmentProposal(selectId, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(COMPETENCY_PROPOSAL_ASSIGNMENT_REJECT_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(MANAGE_ROLE)
    @PutMapping("/bulk-reject-competency-assignment-proposal")
    public ResponseEntity<?> bulkRejectCompetencyProposal(@RequestBody Set<RoleCompetencyProposalId> selectedIds,
                                                           Authentication authentication) throws Exception {
        collaborationService.bulkRejectCompetencyProposal(selectedIds, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(COMPETENCY_PROPOSAL_ASSIGNMENT_REJECT_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(MANAGE_ROLE)
    @PutMapping("/approve-competency-assignment-proposal")
    public ResponseEntity<?> approveCompetencyAssignmentProposal(@RequestBody RoleCompetencyProposalId selectedId,
                                                                  Authentication authentication) throws Exception {
        collaborationService.approveCompetencyAssignmentProposal(selectedId, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(COMPETENCY_PROPOSAL_ASSIGNMENT_APPROVE_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(MANAGE_ROLE)
    @PutMapping("/bulk-approve-competency-assignment-proposal")
    public ResponseEntity<?> bulkApproveCompetencyProposal(@RequestBody Set<RoleCompetencyProposalId> selectedIds,
                                                            Authentication authentication) throws Exception {
        collaborationService.bulkApproveCompetencyProposal(selectedIds, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(COMPETENCY_PROPOSAL_ASSIGNMENT_APPROVE_OK, null, Locale.getDefault())));
    }
}
