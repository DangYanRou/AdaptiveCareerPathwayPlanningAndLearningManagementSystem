package com.tbm.careerpathlearning.controller;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tbm.careerpathlearning.dto.AssignCareerPathwayRequestDto;
import com.tbm.careerpathlearning.service.CareerPathwayAssignmentService;

@RestController
@RequestMapping("/api/career-pathway-assignment")
public class CareerPathwayAssignmentController {

    @Autowired
    private CareerPathwayAssignmentService careerPathwayAssignmentService;

    @Autowired
    private MessageSource messageSource;

    private static final String CAREER_PATHWAY_ASSIGNMENT_CREATION_OK = "career.pathway.assignment.creation.ok.msg";

    private static final String CAREER_PATHWAY_ASSIGNMENT_UPDATE_OK = "career.pathway.assignment.update.ok.msg";

    private static final String CAREER_PATHWAY_DELETE_OK = "career.pathway.assignment.delete.ok.msg";

    private static final String INTERNAL_SERVER_ERROR_EXP_ERR_TITLE_CODE = "internal.server.err.title";

    private static final String EXPORT_ERR_MSG_CODE = "export.data.err.msg";
    
    private static final String IMPORT_OK = "data.import.ok.msg";

    private static final String AUTHORITY_EXPRESSION = """
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_STAFF.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_CAREER_PATHWAY.getAuthorityName()
            )
            """;

    @PreAuthorize(AUTHORITY_EXPRESSION)
    @GetMapping("/overview")
    public ResponseEntity<?> overview() {
        return ResponseEntity.ok(careerPathwayAssignmentService.getOverview());
    }

    @PreAuthorize(AUTHORITY_EXPRESSION)
    @PostMapping()
    public ResponseEntity<?> assignCareerPathway(@RequestBody AssignCareerPathwayRequestDto req,
            Authentication authentication) {
        careerPathwayAssignmentService.assign(req, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(CAREER_PATHWAY_ASSIGNMENT_CREATION_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(AUTHORITY_EXPRESSION)
    @PutMapping("/edit")
    public ResponseEntity<?> update(@RequestBody AssignCareerPathwayRequestDto req,
            Authentication authentication) {
        careerPathwayAssignmentService.update(req, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(CAREER_PATHWAY_ASSIGNMENT_UPDATE_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(AUTHORITY_EXPRESSION)
    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam Long selectedCareerPathwayId,
            Authentication authentication) {
        careerPathwayAssignmentService.delete(selectedCareerPathwayId, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(CAREER_PATHWAY_DELETE_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(AUTHORITY_EXPRESSION)
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<?> bulkDelete(@RequestParam Set<Long> selectedCareerPathwayIds,
            Authentication authentication) {
        careerPathwayAssignmentService.bulkDelete(selectedCareerPathwayIds, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(CAREER_PATHWAY_DELETE_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(AUTHORITY_EXPRESSION)
    @GetMapping("/export")
    public ResponseEntity<?> exportRoleAssignmentData() {
        try {
            ByteArrayResource resource = careerPathwayAssignmentService.exportData();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Exported_Career_Pathway_Assignment_Data.xlsx");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            return ResponseEntity.ok().headers(headers).contentLength(resource.contentLength()).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "title", messageSource.getMessage(INTERNAL_SERVER_ERROR_EXP_ERR_TITLE_CODE, null, Locale.getDefault()),
                    "message", messageSource.getMessage(EXPORT_ERR_MSG_CODE, null, Locale.getDefault())
            ));
        }
    }

    @PreAuthorize(AUTHORITY_EXPRESSION)
    @PostMapping("/import")
    public ResponseEntity<?> importData(@RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        careerPathwayAssignmentService.importData(file, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(IMPORT_OK, null, Locale.getDefault())));
    }
}
