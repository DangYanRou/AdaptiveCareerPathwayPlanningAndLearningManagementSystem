package com.tbm.careerpathlearning.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tbm.careerpathlearning.dto.AuthorityDto;
import com.tbm.careerpathlearning.dto.GrantAccessDto;
import com.tbm.careerpathlearning.dto.OrgChartDto;
import com.tbm.careerpathlearning.dto.RoleAuthorityDto;
import com.tbm.careerpathlearning.dto.RoleAuthorityMapDto;
import com.tbm.careerpathlearning.dto.RoleAuthorityOverviewDto;
import com.tbm.careerpathlearning.dto.RoleDto;
import com.tbm.careerpathlearning.dto.TranslatedAuthorityDto;
import com.tbm.careerpathlearning.enums.AuthorityName;
import com.tbm.careerpathlearning.exception.BadRequestException;
import com.tbm.careerpathlearning.exception.DataAccessException;
import com.tbm.careerpathlearning.model.RoleAuthorityId;
import com.tbm.careerpathlearning.service.AccessControlService;
import com.tbm.careerpathlearning.service.AuthorityService;
import com.tbm.careerpathlearning.service.OrgChartService;
import com.tbm.careerpathlearning.service.RoleAuthorityService;
import com.tbm.careerpathlearning.service.RoleService;
import com.tbm.careerpathlearning.service.ValidationService;

@Service
public class AccessControlServiceImpl implements AccessControlService {

    @Autowired
    private RoleService roleService;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private RoleAuthorityService roleAuthorityService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private OrgChartService orgChartService;

    @Autowired
    private ValidationService validationService;

    private static final String RESULT_NOT_FOUND_ERR_MSG_CODE = "database.result.not.found.err.msg";
    private static final String INVALID_REQUEST_BODY_ERR_MSG_CODE = "invalid.request.body.err.msg";
    private static final String GRANT_ACCESS_OPERATION = "Grant Access";
    private static final String EDIT_GRANT_ACCESS_OPERATION = "Edit Granted Access";
    private static final String REMOVE_GRANT_ACCESS_OPERATION = "Remove Granted Access";
    private static final String DEPT_NAME_NOTES = "access.control.department.name.notes";
    private static final String ROLE_NAME_NOTES = "access.control.role.name.notes";
    private static final String USER_NOTES = "auth.role.user.desc";
    private static final String VIEW_ACCESS_CONTROL_NOTES = "auth.can.view.access.control.desc";
    private static final String MANAGE_ACCESS_CONTROL_NOTES = "auth.can.manage.access.control.desc";
    private static final String VIEW_STAFF_NOTES = "auth.can.view.staff.desc";
    private static final String MANAGE_STAFF_NOTES = "auth.can.manage.staff.desc";
    private static final String VIEW_INVISIBLE_ROLE_NOTES = "auth.can.view.invisible.role.desc";
    private static final String MANAGE_ROLE_NOTES = "auth.can.manage.role.desc";
    private static final String MANAGE_COMPETENCY_NOTES = "auth.can.manage.competency.desc";
    private static final String PROPOSE_ROLE_COMPETENCIES_NOTES = "auth.can.propose.role.competencies.desc";
    private static final String MANAGE_CAREER_PATHWAY_NOTES = "auth.can.manage.career.pathway.desc";
    private static final String MANAGE_ORG_CHART_NOTES = "auth.can.manage.org.chart.desc";
    private static final String MANAGE_TRAINING_NOTES = "auth.can.manage.training.desc";
    private static final String ASSIGN_TRAINING_NOTES = "auth.can.assign.training.desc";
    private static final String MANAGE_LEARNING_MATERIAL_NOTES = "auth.can.manage.learning.material.desc";
    private static final String MANAGE_EVALUATION_NOTES = "auth.can.manage.evaluation.desc";
    private static final String MANAGE_EVALUATION_CYCLE_NOTES = "auth.can.manage.evaluation.cycle.desc";
    private static final String DEPT_NAME_COLUMN = "Department Name";
    private static final String ROLE_NAME_COLUMN = "Role Name";
    private static final String USER_COLUMN = "User";
    private static final String VIEW_ACCESS_CONTROL_COLUMN = "View Access Control";
    private static final String MANAGE_ACCESS_CONTROL_COLUMN = "Manage Access Control";
    private static final String VIEW_STAFF_COLUMN = "View Staff Account";
    private static final String MANAGE_STAFF_COLUMN = "Manage Staff Account";
    private static final String VIEW_INVISIBLE_ROLE_COLUMN = "View Invisible Role";
    private static final String MANAGE_ROLE_COLUMN = "Manage Role";
    private static final String MANAGE_COMPETENCY_COLUMN = "Manage Competency";
    private static final String PROPOSE_ROLE_COMPETENCIES_COLUMN = "Propose Role Competencies";
    private static final String MANAGE_CAREER_PATHWAY_COLUMN = "Manage Career Pathway";
    private static final String MANAGE_ORG_CHART_COLUMN = "Manage Org Chart";
    private static final String MANAGE_TRAINING_COLUMN = "Manage Training";
    private static final String ASSIGN_TRAINING_COLUMN = "Assign Training";
    private static final String MANAGE_LEARNING_MATERIAL_COLUMN = "Manage Learning Material";
    private static final String MANAGE_EVALUATION_COLUMN = "Manage Evaluation";
    private static final String MANAGE_EVALUATION_CYCLE_COLUMN = "Manage Evaluation Cycle";
    private static final String IMPORT_OPERATION = "Import Role Assignment Data";
    private static final String IMPORT_INVALID_DATA_ERR_MSG_CODE = "import.invalid.data.err.msg";
    private static final String IMPORT_FILE_TOO_LARGE_ERR_MSG_CODE = "import.file.too.large.err.msg";
    private static final String IMPORT_FILE_INVALID_ERR_MSG_CODE = "import.file.invalid.err.msg";
    private static final String IMPORT_DUPLICATE_ENTRY_ERR_MSG_CODE = "import.duplicate.entry.err.msg";
    private static final String IMPORT_DATA_NOT_FOUND_ERR_MSG_CODE = "import.data.not.found.err.msg";
    private static final String YES = "Yes";
    private static final int MAX_FILE_SIZE_IN_MB = 5;
    private static final String ACCEPTED_IMPORT_FILE_TYPE = ".xlsx";

    @Override
    public Map<String, Object> getOverview() {
        List<TranslatedAuthorityDto> translatedAuthorityDtoList = authorityService.getAllTranslatedAuthorities();

        if (translatedAuthorityDtoList.isEmpty()) {
            throw new DataAccessException(messageSource.getMessage(RESULT_NOT_FOUND_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        List<RoleDto> roleWithPermissionList = roleService.getAllByDeletedIsFalse();
        Map<Long, RoleAuthorityOverviewDto> overviewMap = new HashMap<>();

        for (RoleDto roleDto : roleWithPermissionList) {
            Long roleId = roleDto.getId();
            OrgChartDto orgChartDto = roleDto.getOrgChart();

            Map<Long, Boolean> roleAuthorityMap = roleAuthorityService.getRoleAuthorityMapByRoleId(roleId);

            RoleAuthorityMapDto roleAuthorityMapDto = new RoleAuthorityMapDto();
            roleAuthorityMapDto.setRoleId(roleId);
            roleAuthorityMapDto.setRoleName(roleDto.getName());
            roleAuthorityMapDto.setAuthorityMap(roleAuthorityMap);

            RoleAuthorityOverviewDto overviewDto = overviewMap.computeIfAbsent(
                    orgChartDto.getId(),
                    id -> new RoleAuthorityOverviewDto(
                            orgChartDto.getId(),
                            orgChartDto.getName(),
                            orgChartDto.isDeleted(),
                            new ArrayList<>()
                    )
            );
            overviewDto.getRoles().add(roleAuthorityMapDto);
        }

        return Map.of(
                "permissions", translatedAuthorityDtoList,
                "departments", new ArrayList<>(overviewMap.values())
        );
    }

    @Override
    @Transactional
    public void grantAccess(GrantAccessDto grantAccessDto, UUID userId) {
        if (grantAccessDto == null || grantAccessDto.getOrgChartId() == null || grantAccessDto.getRoleId() == null
                || grantAccessDto.getSelectedAuthorities() == null || grantAccessDto.getSelectedAuthorities().isEmpty()) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{GRANT_ACCESS_OPERATION}, Locale.getDefault()));
        }

        Long orgChartId = grantAccessDto.getOrgChartId();
        Long roleId = grantAccessDto.getRoleId();
        List<Long> selectedAuthorities = grantAccessDto.getSelectedAuthorities();

        RoleDto roleDto = roleService.getAllById(roleId);

        if (!Objects.equals(roleDto.getOrgChart().getId(), orgChartId)) {
            throw new DataAccessException(messageSource.getMessage(RESULT_NOT_FOUND_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        List<AuthorityDto> allAuthorities = authorityService.findAll();

        List<RoleAuthorityDto> existedRoleAuthority = roleAuthorityService.getAllByRoleId(roleId);
        Set<Long> existedAuthorityIds = existedRoleAuthority.stream()
                .map(dto -> dto.getId().getAuthorityId())
                .collect(Collectors.toSet());

        Set<Long> toAdd = new HashSet<>(selectedAuthorities);
        toAdd.removeAll(existedAuthorityIds);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(8));

        if (!toAdd.isEmpty()) {
            List<RoleAuthorityDto> roleAuthorityDtoList = toAdd.stream()
                    .map(authorityId -> new RoleAuthorityDto(
                    new RoleAuthorityId(roleId, authorityId),
                    roleDto,
                    Objects.requireNonNull(allAuthorities.stream().filter(dto -> dto.getId().equals(authorityId))
                            .findFirst().orElse(null)),
                    userId,
                    now,
                    userId,
                    now
            ))
                    .collect(Collectors.toList());

            roleAuthorityService.createAll(roleAuthorityDtoList);
        }
    }

    @Override
    @Transactional
    public void editGrantedAccess(GrantAccessDto grantAccessDto, UUID userId) {
        if (grantAccessDto == null || grantAccessDto.getOrgChartId() == null || grantAccessDto.getRoleId() == null) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{EDIT_GRANT_ACCESS_OPERATION}, Locale.getDefault()));
        }

        Long orgChartId = grantAccessDto.getOrgChartId();
        Long roleId = grantAccessDto.getRoleId();
        List<Long> selectedAuthorities = grantAccessDto.getSelectedAuthorities();

        RoleDto roleDto = roleService.getAllById(roleId);

        if (!Objects.equals(roleDto.getOrgChart().getId(), orgChartId)) {
            throw new DataAccessException(messageSource.getMessage(RESULT_NOT_FOUND_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        List<RoleAuthorityDto> existedRoleAuthorityList = roleAuthorityService.getAllByRoleId(roleId);
        Set<Long> existedAuthorityIds = existedRoleAuthorityList.stream()
                .map(existedRoleAuthority -> existedRoleAuthority.getAuthority().getId())
                .collect(Collectors.toSet());

        Set<Long> selectedAuthorityIds = new HashSet<>(selectedAuthorities);

        if (Objects.equals(existedAuthorityIds, selectedAuthorityIds)) {
            return;
        }

        Set<Long> toAdd = new HashSet<>(selectedAuthorities);
        toAdd.removeAll(existedAuthorityIds);

        Set<Long> toRemove = new HashSet<>(existedAuthorityIds);
        toRemove.removeAll(selectedAuthorityIds);

        if (!toAdd.isEmpty()) {
            List<RoleAuthorityDto> roleAuthorityDtoToAddList = new ArrayList<>();
            for (Long selectedAuthorityId : toAdd) {
                RoleAuthorityDto roleAuthorityDto = new RoleAuthorityDto();
                RoleAuthorityId roleAuthorityId = new RoleAuthorityId(roleId, selectedAuthorityId);
                AuthorityDto authorityDto = authorityService.findById(selectedAuthorityId);

                roleAuthorityDto.setId(roleAuthorityId);
                roleAuthorityDto.setRole(roleDto);
                roleAuthorityDto.setAuthority(authorityDto);
                roleAuthorityDto.setCreatedAt(OffsetDateTime.now());
                roleAuthorityDto.setUpdatedAt(OffsetDateTime.now());
                roleAuthorityDto.setCreatedBy(userId);
                roleAuthorityDto.setUpdatedBy(userId);

                roleAuthorityDtoToAddList.add(roleAuthorityDto);
            }

            roleAuthorityService.createAll(roleAuthorityDtoToAddList);
        }

        if (!toRemove.isEmpty()) {
            roleAuthorityService.deleteByRoleIdAndAuthorityIdIn(roleId, toRemove);
        }
    }

    @Override
    @Transactional
    public void deleteRoleAuthority(Long orgChartId, Long roleId) {
        if (orgChartId == null || roleId == null) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{REMOVE_GRANT_ACCESS_OPERATION}, Locale.getDefault()));
        }

        RoleDto roleDto = roleService.getAllById(roleId);

        if (!Objects.equals(roleDto.getOrgChart().getId(), orgChartId)) {
            throw new DataAccessException(messageSource.getMessage(RESULT_NOT_FOUND_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        List<RoleAuthorityDto> existedRoleAuthorityList = roleAuthorityService.getAllByRoleId(roleId);

        if (existedRoleAuthorityList.isEmpty()) {
            throw new DataAccessException(messageSource.getMessage(RESULT_NOT_FOUND_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        Set<RoleAuthorityId> existedRoleAuthorityIds = existedRoleAuthorityList.stream()
                .map(RoleAuthorityDto::getId)
                .collect(Collectors.toSet());

        roleAuthorityService.deleteAllByIdIn(existedRoleAuthorityIds);
    }

    @Override
    @Transactional
    public void bulkDeleteRoleAuthority(List<Long> selectedRoleIds) {
        if (selectedRoleIds == null || selectedRoleIds.isEmpty()) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{REMOVE_GRANT_ACCESS_OPERATION}, Locale.getDefault()));
        }

        Set<Long> selectedRoleIdsSet = new HashSet<>(selectedRoleIds);

        List<RoleDto> roleDtoList = roleService.getAllByIsDeletedIsFalseAndIdIn(selectedRoleIdsSet);

        if (roleDtoList.size() != selectedRoleIds.size()) {
            throw new DataAccessException(messageSource.getMessage(RESULT_NOT_FOUND_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        List<RoleAuthorityDto> existedRoleAuthorityList = roleAuthorityService.getAllByRoleIdIn(selectedRoleIdsSet);

        if (existedRoleAuthorityList.isEmpty()) {
            throw new DataAccessException(messageSource.getMessage(RESULT_NOT_FOUND_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        roleAuthorityService.deleteAllByRoleIdIn(selectedRoleIdsSet);
    }

    @Override
    public ByteArrayResource exportData() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Sheet 1");
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            int rowIndex = 0;

            Row header = sheet.createRow(rowIndex);
            addHeaderCell(drawing, header, 0, DEPT_NAME_COLUMN, DEPT_NAME_NOTES);
            addHeaderCell(drawing, header, 1, ROLE_NAME_COLUMN, ROLE_NAME_NOTES);
            addHeaderCell(drawing, header, 2, USER_COLUMN, USER_NOTES);
            addHeaderCell(drawing, header, 3, VIEW_ACCESS_CONTROL_COLUMN, VIEW_ACCESS_CONTROL_NOTES);
            addHeaderCell(drawing, header, 4, MANAGE_ACCESS_CONTROL_COLUMN, MANAGE_ACCESS_CONTROL_NOTES);
            addHeaderCell(drawing, header, 5, VIEW_STAFF_COLUMN, VIEW_STAFF_NOTES);
            addHeaderCell(drawing, header, 6, MANAGE_STAFF_COLUMN, MANAGE_STAFF_NOTES);
            addHeaderCell(drawing, header, 7, VIEW_INVISIBLE_ROLE_COLUMN, VIEW_INVISIBLE_ROLE_NOTES);
            addHeaderCell(drawing, header, 8, MANAGE_ROLE_COLUMN, MANAGE_ROLE_NOTES);
            addHeaderCell(drawing, header, 9, MANAGE_COMPETENCY_COLUMN, MANAGE_COMPETENCY_NOTES);
            addHeaderCell(drawing, header, 10, PROPOSE_ROLE_COMPETENCIES_COLUMN, PROPOSE_ROLE_COMPETENCIES_NOTES);
            addHeaderCell(drawing, header, 11, MANAGE_CAREER_PATHWAY_COLUMN, MANAGE_CAREER_PATHWAY_NOTES);
            addHeaderCell(drawing, header, 12, MANAGE_ORG_CHART_COLUMN, MANAGE_ORG_CHART_NOTES);
            addHeaderCell(drawing, header, 13, MANAGE_TRAINING_COLUMN, MANAGE_TRAINING_NOTES);
            addHeaderCell(drawing, header, 14, ASSIGN_TRAINING_COLUMN, ASSIGN_TRAINING_NOTES);
            addHeaderCell(drawing, header, 15, MANAGE_LEARNING_MATERIAL_COLUMN, MANAGE_LEARNING_MATERIAL_NOTES);
            addHeaderCell(drawing, header, 16, MANAGE_EVALUATION_COLUMN, MANAGE_EVALUATION_NOTES);
            addHeaderCell(drawing, header, 17, MANAGE_EVALUATION_CYCLE_COLUMN, MANAGE_EVALUATION_CYCLE_NOTES);

            rowIndex++;

            List<RoleDto> allRoles = roleService.getAllByDeletedIsFalse();
            List<RoleAuthorityDto> allAuthorities = roleAuthorityService.getAll();

            for (RoleDto dto : allRoles) {
                Set<AuthorityName> assignedAuthorities = allAuthorities.stream()
                        .filter(authority -> Objects.equals(authority.getId().getRoleId(), dto.getId()))
                        .map(authority -> authority.getAuthority().getName())
                        .collect(Collectors.toSet());

                Row row = sheet.createRow(rowIndex);
                row.createCell(0).setCellValue(dto.getOrgChart().getName());
                row.createCell(1).setCellValue(dto.getName());
                row.createCell(2).setCellValue(assignedAuthorities.contains(AuthorityName.ROLE_USER) ? YES : null);
                row.createCell(3).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_VIEW_ACCESS_CONTROL) ? YES : null);
                row.createCell(4).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_MANAGE_ACCESS_CONTROL) ? YES : null);
                row.createCell(5).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_VIEW_STAFF) ? YES : null);
                row.createCell(6).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_MANAGE_STAFF) ? YES : null);
                row.createCell(7).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_VIEW_INVISIBLE_ROLE) ? YES : null);
                row.createCell(8).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_MANAGE_ROLE) ? YES : null);
                row.createCell(9).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_MANAGE_COMPETENCY) ? YES : null);
                row.createCell(10).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_PROPOSE_ROLE_COMPETENCIES) ? YES : null);
                row.createCell(11).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_MANAGE_CAREER_PATHWAY) ? YES : null);
                row.createCell(12).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_MANAGE_ORG_CHART) ? YES : null);
                row.createCell(13).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_MANAGE_TRAINING) ? YES : null);
                row.createCell(14).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_ASSIGN_TRAINING) ? YES : null);
                row.createCell(15).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_MANAGE_LEARNING_MATERIAL) ? YES : null);
                row.createCell(16).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_MANAGE_EVALUATION) ? YES : null);
                row.createCell(17).setCellValue(assignedAuthorities.contains(AuthorityName.CAN_MANAGE_EVALUATION_CYCLE) ? YES : null);

                rowIndex++;
            }

            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());
        }
    }

    @Override
    @Transactional
    public void importData(MultipartFile file, UUID userId) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{IMPORT_OPERATION}, Locale.getDefault()));
        }

        if (file.getSize() > MAX_FILE_SIZE_IN_MB * 1024 * 1024) {
            throw new BadRequestException(messageSource.getMessage(IMPORT_FILE_TOO_LARGE_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        List<String> acceptedTypes = List.of(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/octet-stream",
                ""
        );

        if (!acceptedTypes.contains(file.getContentType())) {
            throw new BadRequestException(messageSource.getMessage(IMPORT_FILE_INVALID_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(ACCEPTED_IMPORT_FILE_TYPE)) {
            throw new BadRequestException(messageSource.getMessage(IMPORT_FILE_INVALID_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(8));

        List<OrgChartDto> allDepartment = orgChartService.findAllByIsDeletedIsFalse();
        List<RoleDto> allRoles = roleService.getAllByDeletedIsFalse();
        Map<String, Set<String>> allDepartmentRoleNameMap = allDepartment.stream().collect(Collectors.toMap(
                dto -> dto.getName().trim().toLowerCase(),
                dto -> allRoles.stream()
                        .filter(role -> Objects.equals(role.getOrgChart().getId(), dto.getId()))
                        .map(role -> role.getName().toLowerCase().trim())
                        .collect(Collectors.toSet())));

        List<RoleAuthorityDto> allRoleAuthorities = roleAuthorityService.getAll();
        List<AuthorityDto> allAuthority = authorityService.findAll();

        Map<AuthorityName, Long> allAuthorityNameMap = allAuthority.stream().collect(Collectors.toMap(
                AuthorityDto::getName, AuthorityDto::getId));
        Map<Long, AuthorityDto> allAuthorityMap = allAuthority.stream().collect(Collectors.toMap(
                AuthorityDto::getId, Function.identity()));

        Sheet sheet = workbook.getSheetAt(0);

        Row header = sheet.getRow(0);
        if (!getCellValueAsString(header.getCell(0)).equalsIgnoreCase(DEPT_NAME_COLUMN)
                || !getCellValueAsString(header.getCell(1)).equalsIgnoreCase(ROLE_NAME_COLUMN)
                || !getCellValueAsString(header.getCell(2)).equalsIgnoreCase(USER_COLUMN)
                || !getCellValueAsString(header.getCell(3)).equalsIgnoreCase(VIEW_ACCESS_CONTROL_COLUMN)
                || !getCellValueAsString(header.getCell(4)).equalsIgnoreCase(MANAGE_ACCESS_CONTROL_COLUMN)
                || !getCellValueAsString(header.getCell(5)).equalsIgnoreCase(VIEW_STAFF_COLUMN)
                || !getCellValueAsString(header.getCell(6)).equalsIgnoreCase(MANAGE_STAFF_COLUMN)
                || !getCellValueAsString(header.getCell(7)).equalsIgnoreCase(VIEW_INVISIBLE_ROLE_COLUMN)
                || !getCellValueAsString(header.getCell(8)).equalsIgnoreCase(MANAGE_ROLE_COLUMN)
                || !getCellValueAsString(header.getCell(9)).equalsIgnoreCase(MANAGE_COMPETENCY_COLUMN)
                || !getCellValueAsString(header.getCell(10)).equalsIgnoreCase(PROPOSE_ROLE_COMPETENCIES_COLUMN)
                || !getCellValueAsString(header.getCell(11)).equalsIgnoreCase(MANAGE_CAREER_PATHWAY_COLUMN)
                || !getCellValueAsString(header.getCell(12)).equalsIgnoreCase(MANAGE_ORG_CHART_COLUMN)
                || !getCellValueAsString(header.getCell(13)).equalsIgnoreCase(MANAGE_TRAINING_COLUMN)
                || !getCellValueAsString(header.getCell(14)).equalsIgnoreCase(ASSIGN_TRAINING_COLUMN)
                || !getCellValueAsString(header.getCell(15)).equalsIgnoreCase(MANAGE_LEARNING_MATERIAL_COLUMN)
                || !getCellValueAsString(header.getCell(16)).equalsIgnoreCase(MANAGE_EVALUATION_COLUMN)
                || !getCellValueAsString(header.getCell(17)).equalsIgnoreCase(MANAGE_EVALUATION_CYCLE_COLUMN)) {
            throw new BadRequestException(messageSource.getMessage(IMPORT_FILE_INVALID_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        Map<String, Set<String>> duplicatedRow = new HashMap<>();
        List<RoleAuthorityDto> roleAuthorityDtoToCreate = new ArrayList<>();
        Set<RoleAuthorityId> roleAuthorityIdToDelete = new HashSet<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || row.getCell(0) == null || getCellValueAsString(row.getCell(0)) == null
                    || getCellValueAsString(row.getCell(0)).isBlank()) {
                continue;
            }

            String departmentName = getCellValueAsString(row.getCell(0));
            String roleName = getCellValueAsString(row.getCell(1));
            String user = getCellValueAsString(row.getCell(2));
            String viewAccessControl = getCellValueAsString(row.getCell(3));
            String manageAccessControl = getCellValueAsString(row.getCell(4));
            String viewStaffAccount = getCellValueAsString(row.getCell(5));
            String manageStaffAccount = getCellValueAsString(row.getCell(6));
            String viewInvisibleRole = getCellValueAsString(row.getCell(7));
            String manageRole = getCellValueAsString(row.getCell(8));
            String manageCompetency = getCellValueAsString(row.getCell(9));
            String proposeRoleCompetency = getCellValueAsString(row.getCell(10));
            String manageCareerPathway = getCellValueAsString(row.getCell(11));
            String manageOrgChart = getCellValueAsString(row.getCell(12));
            String manageTraining = getCellValueAsString(row.getCell(13));
            String assignTraining = getCellValueAsString(row.getCell(14));
            String manageLearning = getCellValueAsString(row.getCell(15));
            String manageEvaluation = getCellValueAsString(row.getCell(16));
            String manageEvaluationCycle = getCellValueAsString(row.getCell(17));

            if (validationService.isNullOrBlank(departmentName)) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE,
                        new String[]{DEPT_NAME_COLUMN, Integer.toString(i + 1)}, Locale.getDefault()));
            }

            if (validationService.isNullOrBlank(roleName)) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE,
                        new String[]{ROLE_NAME_COLUMN, Integer.toString(i + 1)}, Locale.getDefault()));
            }

            if (!allDepartmentRoleNameMap.containsKey(departmentName.trim().toLowerCase())) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_DATA_NOT_FOUND_ERR_MSG_CODE, new String[]{departmentName}, Locale.getDefault()));
            }

            if (!allDepartmentRoleNameMap.get(departmentName.trim().toLowerCase()).contains(roleName.trim().toLowerCase())) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_DATA_NOT_FOUND_ERR_MSG_CODE, new String[]{roleName}, Locale.getDefault()));
            }

            if (duplicatedRow.get(departmentName.toLowerCase().trim()) != null
                    && duplicatedRow.get(departmentName.toLowerCase().trim()).contains(roleName.toLowerCase().trim())) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_DUPLICATE_ENTRY_ERR_MSG_CODE, new String[]{Integer.toString(i + 1)}, Locale.getDefault()));
            }

            duplicatedRow.computeIfAbsent(departmentName.toLowerCase().trim(), k -> new HashSet<>())
                    .add(roleName.toLowerCase().trim());

            RoleDto selectedRoleDto = Objects.requireNonNull(
                    allRoles.stream().filter(dto -> roleName.trim().equalsIgnoreCase(dto.getName().trim()))
                            .findFirst().orElse(null));
            Long roleId = selectedRoleDto.getId();

            Set<RoleAuthorityId> assignedAuthority = allRoleAuthorities.stream().map(RoleAuthorityDto::getId)
                    .filter(id -> Objects.equals(id.getRoleId(), selectedRoleDto.getId()))
                    .collect(Collectors.toSet());

            Set<RoleAuthorityId> inputtedAuthority = new HashSet<>();
            addIfYes(inputtedAuthority, user, roleId, AuthorityName.ROLE_USER, allAuthorityNameMap);
            addIfYes(inputtedAuthority, viewAccessControl, roleId, AuthorityName.CAN_VIEW_ACCESS_CONTROL, allAuthorityNameMap);
            addIfYes(inputtedAuthority, manageAccessControl, roleId, AuthorityName.CAN_MANAGE_ACCESS_CONTROL, allAuthorityNameMap);
            addIfYes(inputtedAuthority, viewStaffAccount, roleId, AuthorityName.CAN_VIEW_STAFF, allAuthorityNameMap);
            addIfYes(inputtedAuthority, manageStaffAccount, roleId, AuthorityName.CAN_MANAGE_STAFF, allAuthorityNameMap);
            addIfYes(inputtedAuthority, viewInvisibleRole, roleId, AuthorityName.CAN_VIEW_INVISIBLE_ROLE, allAuthorityNameMap);
            addIfYes(inputtedAuthority, manageRole, roleId, AuthorityName.CAN_MANAGE_ROLE, allAuthorityNameMap);
            addIfYes(inputtedAuthority, manageCompetency, roleId, AuthorityName.CAN_MANAGE_COMPETENCY, allAuthorityNameMap);
            addIfYes(inputtedAuthority, proposeRoleCompetency, roleId, AuthorityName.CAN_PROPOSE_ROLE_COMPETENCIES, allAuthorityNameMap);
            addIfYes(inputtedAuthority, manageCareerPathway, roleId, AuthorityName.CAN_MANAGE_CAREER_PATHWAY, allAuthorityNameMap);
            addIfYes(inputtedAuthority, manageOrgChart, roleId, AuthorityName.CAN_MANAGE_ORG_CHART, allAuthorityNameMap);
            addIfYes(inputtedAuthority, manageTraining, roleId, AuthorityName.CAN_MANAGE_TRAINING, allAuthorityNameMap);
            addIfYes(inputtedAuthority, assignTraining, roleId, AuthorityName.CAN_ASSIGN_TRAINING, allAuthorityNameMap);
            addIfYes(inputtedAuthority, manageLearning, roleId, AuthorityName.CAN_MANAGE_LEARNING_MATERIAL, allAuthorityNameMap);
            addIfYes(inputtedAuthority, manageEvaluation, roleId, AuthorityName.CAN_MANAGE_EVALUATION, allAuthorityNameMap);
            addIfYes(inputtedAuthority, manageEvaluationCycle, roleId, AuthorityName.CAN_MANAGE_EVALUATION_CYCLE, allAuthorityNameMap);

            Set<RoleAuthorityId> toAdd = new HashSet<>(inputtedAuthority);
            toAdd.removeAll(assignedAuthority);

            Set<RoleAuthorityId> toRemove = new HashSet<>(assignedAuthority);
            toRemove.removeAll(inputtedAuthority);
            roleAuthorityIdToDelete.addAll(toRemove);

            if (!toAdd.isEmpty()) {
                roleAuthorityDtoToCreate.addAll(toAdd.stream().map(id
                        -> new RoleAuthorityDto(id, selectedRoleDto, allAuthorityMap.get(id.getAuthorityId()), userId, now, userId, now))
                        .collect(Collectors.toSet()));
            }
        }

        if (!roleAuthorityDtoToCreate.isEmpty()) {
            roleAuthorityService.createAll(roleAuthorityDtoToCreate);
        }

        if (!roleAuthorityIdToDelete.isEmpty()) {
            roleAuthorityService.deleteAllByIdIn(roleAuthorityIdToDelete);
        }
    }

    private void addIfYes(Set<RoleAuthorityId> target, String cellValue, Long roleId,
            AuthorityName authorityName, Map<AuthorityName, Long> authorityNameMap) {
        if (!validationService.isNullOrBlank(cellValue) && cellValue.equalsIgnoreCase(YES)) {
            target.add(new RoleAuthorityId(roleId, authorityNameMap.get(authorityName)));
        }
    }

    private void addHeaderCell(XSSFDrawing drawing, Row header, int colIndex, String title, String notesMsgCode) {
        Cell cell = header.createCell(colIndex);
        cell.setCellValue(title);
        createCellComment(drawing, cell, messageSource.getMessage(notesMsgCode, null, Locale.getDefault()));
    }

    private static void createCellComment(XSSFDrawing drawing, Cell cell, String text) {
        CreationHelper factory = cell.getSheet().getWorkbook().getCreationHelper();
        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 3);
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(cell.getRowIndex() + 3);
        Comment comment = drawing.createCellComment(anchor);
        RichTextString str = factory.createRichTextString(text);
        comment.setString(str);
        comment.setAuthor("System");
        cell.setCellComment(comment);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (null == cell.getCellType()) {
            return cell.getStringCellValue().trim();
        }
        return switch (cell.getCellType()) {
            case NUMERIC ->
                String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN ->
                String.valueOf(cell.getBooleanCellValue());
            default ->
                cell.getStringCellValue().trim();
        };
    }
}
