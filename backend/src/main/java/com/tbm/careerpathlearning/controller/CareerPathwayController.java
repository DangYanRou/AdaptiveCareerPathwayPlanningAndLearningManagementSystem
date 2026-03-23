package com.tbm.careerpathlearning.controller;

import com.tbm.careerpathlearning.dto.CreateCareerPathwayRequestDto;
import com.tbm.careerpathlearning.service.CareerPathwayManagementService;
import com.tbm.careerpathlearning.service.CareerPathwayService;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/career-pathway")
public class CareerPathwayController {

    @Autowired
    private CareerPathwayService careerPathwayService;

    @Autowired
    private CareerPathwayManagementService careerPathwayManagementService;

    @Autowired
    private MessageSource messageSource;

    private static final String CAREER_PATHWAY_CREATION_OK = "career.pathway.creation.ok.msg";
    private static final String CAREER_PATHWAY_UPDATE_OK = "career.pathway.update.ok.msg";
    private static final String CAREER_PATHWAY_DELETE_OK = "career.pathway.delete.ok.msg";
    private static final String INTERNAL_SERVER_ERROR_EXP_ERR_TITLE_CODE = "internal.server.err.title";
    private static final String EXPORT_ERR_MSG_CODE = "export.data.err.msg";
    private static final String IMPORT_OK = "data.import.ok.msg";

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_CAREER_PATHWAY.getAuthorityName(),
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_STAFF.getAuthorityName()
            )
            """)
    @GetMapping()
    public ResponseEntity<?> getAll(Authentication authentication) {
        return ResponseEntity.ok(careerPathwayService.getAllByIsDeletedIsFalse());
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_CAREER_PATHWAY.getAuthorityName()
            )
            """)
    @GetMapping("/overview")
    public ResponseEntity<?> overview(Authentication authentication) {
        return ResponseEntity.ok(careerPathwayManagementService.getOverview());
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_CAREER_PATHWAY.getAuthorityName()
            )
            """)
    @PostMapping()
    public ResponseEntity<?> createCareerPathway(@RequestBody CreateCareerPathwayRequestDto req,
                                                 Authentication authentication) {
        careerPathwayManagementService.create(req, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(CAREER_PATHWAY_CREATION_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_CAREER_PATHWAY.getAuthorityName()
            )
            """)
    @PutMapping("/edit")
    public ResponseEntity<?> update(@RequestBody CreateCareerPathwayRequestDto req,
                                    Authentication authentication) {
        careerPathwayManagementService.update(req, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(CAREER_PATHWAY_UPDATE_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_CAREER_PATHWAY.getAuthorityName()
            )
            """)
    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam Long selectedCareerPathwayId,
                                    Authentication authentication) {
        careerPathwayManagementService.delete(selectedCareerPathwayId, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(CAREER_PATHWAY_DELETE_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_CAREER_PATHWAY.getAuthorityName()
            )
            """)
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<?> bulkDelete(@RequestParam Set<Long> selectedCareerPathwayIds,
                                        Authentication authentication) {
        careerPathwayManagementService.bulkDelete(selectedCareerPathwayIds, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(CAREER_PATHWAY_DELETE_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_CAREER_PATHWAY.getAuthorityName()
            )
            """)
    @GetMapping("/export")
    public ResponseEntity<?> exportCareerPathwayOverviewData(Authentication authentication) {
        try {
            byte[] data = careerPathwayManagementService.exportData();
            ByteArrayResource resource = new ByteArrayResource(data);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Exported_Career_Pathway_Overview_Data.xlsx");
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
                T(com.tbm.careerpathlearning.enums.AuthorityName).CAN_MANAGE_CAREER_PATHWAY.getAuthorityName()
            )
            """)
    @PostMapping("/import")
    public ResponseEntity<?> importData(@RequestParam("file") MultipartFile file,
                                        Authentication authentication) throws IOException {
        careerPathwayManagementService.importData(file, UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(Map.of(
                "message", messageSource.getMessage(IMPORT_OK, null, Locale.getDefault())
        ));
    }

    @PreAuthorize("""
            hasAnyAuthority(
                T(com.tbm.careerpathlearning.enums.AuthorityName).ROLE_USER.getAuthorityName()
            )
            """)
    @GetMapping("/my")
    public ResponseEntity<?> my(@RequestParam UUID staffId, Authentication authentication) {
        return ResponseEntity.ok(careerPathwayManagementService.getMyCareerPathway(staffId));
    }
}
