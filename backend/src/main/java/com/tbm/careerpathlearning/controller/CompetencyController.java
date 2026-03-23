package com.tbm.careerpathlearning.controller;

import com.tbm.careerpathlearning.dto.CreateCompetencyRequestDto;
import com.tbm.careerpathlearning.dto.EditCompetencyRequestDto;
import com.tbm.careerpathlearning.service.CompetencyManagementService;
import com.tbm.careerpathlearning.service.CompetencyService;
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

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/competency")
public class CompetencyController {

    @Autowired
    private CompetencyManagementService competencyManagementService;

    @Autowired
    private CompetencyService competencyService;

    @Autowired
    private MessageSource messageSource;

    private static final String COMPETENCY_CREATION_OK = "competency.creation.ok.msg";
    private static final String COMPETENCY_EDIT_OK = "competency.edit.ok.msg";
    private static final String COMPETENCY_DELETE_OK = "competency.delete.ok.msg";
    private static final String IMPORT_OK = "data.import.ok.msg";
    private static final String INTERNAL_SERVER_ERROR_EXP_ERR_TITLE_CODE = "internal.server.err.title";
    private static final String EXPORT_ERR_MSG_CODE = "export.data.err.msg";

    private static final String VIEW_COMPETENCY = """
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_ROLE.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_COMPETENCY.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_PROPOSE_ROLE_COMPETENCIES.getAuthorityName()
            )
            """;

    private static final String MANAGE_COMPETENCY = """
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_COMPETENCY.getAuthorityName()
            )
            """;

    @GetMapping
    public ResponseEntity<?> getAllCompetencies() {
        return ResponseEntity.ok(competencyService.findAllByIsDeletedIsFalse());
    }

    @PreAuthorize(VIEW_COMPETENCY)
    @GetMapping("/overview")
    public ResponseEntity<?> getCompetencyOverview() {
        return ResponseEntity.ok(competencyManagementService.getOverview());
    }

    @PreAuthorize(MANAGE_COMPETENCY)
    @PostMapping
    public ResponseEntity<?> createCompetency(@RequestBody CreateCompetencyRequestDto requestDto, Authentication authentication) {
        competencyManagementService.create(requestDto, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(COMPETENCY_CREATION_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(MANAGE_COMPETENCY)
    @PutMapping("/edit-competency")
    public ResponseEntity<?> updateCompetency(@RequestBody EditCompetencyRequestDto requestDto, Authentication authentication) {
        competencyManagementService.update(requestDto, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(COMPETENCY_EDIT_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(MANAGE_COMPETENCY)
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteCompetency(@RequestParam Long competencyId, Authentication authentication) {
        competencyManagementService.delete(competencyId, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(COMPETENCY_DELETE_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(MANAGE_COMPETENCY)
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<?> bulkDeleteCompetency(@RequestParam List<Long> competencyIds, Authentication authentication) {
        competencyManagementService.bulkDelete(competencyIds, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(COMPETENCY_DELETE_OK, null, Locale.getDefault())));
    }

    @PreAuthorize(MANAGE_COMPETENCY)
    @GetMapping("/export")
    public ResponseEntity<?> exportCompetencyOverviewData() {
        try {
            ByteArrayResource resource = competencyManagementService.exportData();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Exported_Competency_Overview_Data.xlsx");
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

    @PreAuthorize(MANAGE_COMPETENCY)
    @PostMapping("/import")
    public ResponseEntity<?> importData(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        competencyManagementService.importData(file, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of("message", messageSource.getMessage(IMPORT_OK, null, Locale.getDefault())));
    }
}
