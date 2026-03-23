package com.tbm.careerpathlearning.controller;

import com.tbm.careerpathlearning.dto.EditCompetencyProposalRequestDto;
import com.tbm.careerpathlearning.dto.ProposeCompetencyRequest;
import com.tbm.careerpathlearning.model.ProposalParticipantId;
import com.tbm.careerpathlearning.service.CompetencyDefinitionCollaborationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/competency-definition-collaboration")
@CrossOrigin
public class CompetencyDefinitionCollaborationController {

    @Autowired
    private CompetencyDefinitionCollaborationService collaborationService;

    @Autowired
    private MessageSource messageSource;

    private static final String COMPETENCY_PROPOSAL_CREATION_OK = "proposal.competency.creation.ok.msg";
    private static final String COMPETENCY_PROPOSAL_EDIT_OK = "proposal.competency.edit.ok.msg";
    private static final String COMPETENCY_PROPOSAL_REJECT_OK = "proposal.competency.reject.ok.msg";
    private static final String COMPETENCY_PROPOSAL_APPROVE_OK = "proposal.competency.approve.ok.msg";

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_COMPETENCY.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_PROPOSE_ROLE_COMPETENCIES.getAuthorityName()
            )
            """)
    @GetMapping("/overview")
    public ResponseEntity<?> competencyProposalOverview(Authentication authentication) {
        return ResponseEntity.ok(collaborationService.getOverview(UUID.fromString(authentication.getName())));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_COMPETENCY.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_PROPOSE_ROLE_COMPETENCIES.getAuthorityName()
            )
            """)
    @PostMapping()
    public ResponseEntity<?> proposeCompetency(@RequestBody ProposeCompetencyRequest req,
                                               Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        collaborationService.proposeCompetency(req, UUID.fromString(authentication.getName()), roles);
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(COMPETENCY_PROPOSAL_CREATION_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_COMPETENCY.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_PROPOSE_ROLE_COMPETENCIES.getAuthorityName()
            )
            """)
    @PutMapping("/edit-competency-proposal")
    public ResponseEntity<?> updateCompetencyProposal(@RequestBody EditCompetencyProposalRequestDto req,
                                                      Authentication authentication) {
        collaborationService.updateCompetencyProposal(req, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(COMPETENCY_PROPOSAL_EDIT_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_COMPETENCY.getAuthorityName()
            )
            """)
    @PutMapping("/reject-competency-proposal")
    public ResponseEntity<?> rejectCompetencyProposal(@RequestBody ProposalParticipantId selectedProposalParticipantId,
                                                      Authentication authentication) {
        collaborationService.rejectCompetencyProposal(selectedProposalParticipantId, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(COMPETENCY_PROPOSAL_REJECT_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_COMPETENCY.getAuthorityName()
            )
            """)
    @PutMapping("/bulk-reject-competency-proposal")
    public ResponseEntity<?> bulkRejectCompetencyProposal(@RequestBody Set<ProposalParticipantId> selectedProposalParticipantId,
                                                          Authentication authentication) {
        collaborationService.bulkRejectCompetencyProposal(selectedProposalParticipantId, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(COMPETENCY_PROPOSAL_REJECT_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_COMPETENCY.getAuthorityName()
            )
            """)
    @PutMapping("/approve-competency-proposal")
    public ResponseEntity<?> approveCompetencyProposal(@RequestBody ProposalParticipantId selectedProposalParticipantId,
                                                       Authentication authentication) {
        collaborationService.approveCompetencyProposal(selectedProposalParticipantId, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(COMPETENCY_PROPOSAL_APPROVE_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_COMPETENCY.getAuthorityName()
            )
            """)
    @PutMapping("/bulk-approve-competency-proposal")
    public ResponseEntity<?> bulkApproveCompetencyProposal(@RequestBody Set<ProposalParticipantId> selectedProposalParticipantIds,
                                                           Authentication authentication) {
        collaborationService.bulkApproveCompetencyProposal(selectedProposalParticipantIds, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(COMPETENCY_PROPOSAL_APPROVE_OK, null, Locale.getDefault())
        ));
    }
}
