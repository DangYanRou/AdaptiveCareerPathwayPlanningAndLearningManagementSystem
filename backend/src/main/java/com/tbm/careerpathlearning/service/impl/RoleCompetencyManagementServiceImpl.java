package com.tbm.careerpathlearning.service.impl;

import com.tbm.careerpathlearning.dto.*;
import com.tbm.careerpathlearning.exception.BadRequestException;
import com.tbm.careerpathlearning.model.RoleCompetencyId;
import com.tbm.careerpathlearning.service.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoleCompetencyManagementServiceImpl implements RoleCompetencyManagementService {

    @Autowired
    private CompetencyService competencyService;

    @Autowired
    private RoleCompetencyService roleCompetencyService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleJobScopeService roleJobScopeService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private CompetencyCompTagService competencyCompTagService;

    @Autowired
    private OrgChartService orgChartService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private MessageSource messageSource;

    private static final Integer WEIGHTAGE_MAX = 100;
    private static final Integer WEIGHTAGE_MIN = 1;
    private static final String INVALID_REQUEST_BODY_ERR_MSG_CODE = "invalid.request.body.err.msg";
    private static final String COMPETENCY_ASSIGNMENT_OPERATION = "Competency Assignment";
    private static final String COMPETENCY_ASSIGNMENT_DELETE_OPERATION = "Competency Assignment Deletion";
    private static final String VIEW_ROLE_DETAILS = "Viewing role details";
    private static final String DEPT_NAME_NOTES = "competency.assignment.department.name.notes";
    private static final String ROLE_NAME_NOTES = "competency.assignment.role.name.notes";
    private static final String COMPETENCY_NAME_NOTES = "competency.assignment.competency.weightage.notes";
    private static final String DEPT_NAME_COLUMN = "Department Name";
    private static final String ROLE_NAME_COLUMN = "Role Name";
    private static final String COMPETENCY_WEIGHTAGE_COLUMN = "Competency Name > Weightage";
    private static final String WEIGHTAGE_COLUMN = "Weightage";
    private static final String IMPORT_OPERATION = "Import Role Assignment Data";
    private static final String IMPORT_INVALID_DATA_ERR_MSG_CODE = "import.invalid.data.err.msg";
    private static final String IMPORT_FILE_TOO_LARGE_ERR_MSG_CODE = "import.file.too.large.err.msg";
    private static final String IMPORT_FILE_INVALID_ERR_MSG_CODE = "import.file.invalid.err.msg";
    private static final String IMPORT_DUPLICATE_ENTRY_ERR_MSG_CODE = "import.duplicate.entry.err.msg";
    private static final String IMPORT_DATA_NOT_FOUND_ERR_MSG_CODE = "import.data.not.found.err.msg";
    private static final int MAX_FILE_SIZE_IN_MB = 5;
    private static final String ACCEPTED_IMPORT_FILE_TYPE = ".xlsx";

    @Override
    public List<CompetencyAssignmentOverviewDto> getOverview() {
        List<CompetencyAssignmentOverviewDto> competencyAssignmentOverviewDtoList = new ArrayList<>();

        List<RoleDto> existingRoleDtoList = roleService.getAllByDeletedIsFalse();
        List<RoleCompetencyDto> existingRoleCompetencyDtoList = roleCompetencyService.findAll();
        List<CompetencyCompTagDto> allCompTags = competencyCompTagService.findAll();

        existingRoleDtoList.forEach((roleDto) -> {
            Map<Long, CompetencyAssignmentDto> assignedCompetency = existingRoleCompetencyDtoList.stream()
                    .filter(roleCompetencyDto ->
                            roleCompetencyDto.getId().getRoleId().equals(roleDto.getId())
                    ).collect(Collectors.toMap(
                            roleCompetencyDto -> roleCompetencyDto.getId().getCompetencyId(),
                            roleCompetencyDto -> new CompetencyAssignmentDto(
                                    roleCompetencyDto.getId().getCompetencyId(),
                                    roleCompetencyDto.getCompetency().getName(),
                                    roleCompetencyDto.getCompetency().getDescription(),
                                    roleCompetencyDto.getCompetency().isDeleted(),
                                    roleCompetencyDto.getWeightage()
                            )
                    ));

            int totalWeightage = assignedCompetency.values().stream()
                    .mapToInt(CompetencyAssignmentDto::getWeightage)
                    .sum();

            List<CompetencyCompTagDto> assignedCompetencyCompTagDto = allCompTags.stream()
                    .filter(compTag -> assignedCompetency.containsKey(compTag.getId().getCompetencyId()))
                    .toList();

            Map<Long, List<String>> assignedCompTags = assignedCompetency.keySet().stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            id -> assignedCompetencyCompTagDto.stream()
                                    .filter(compTag -> compTag.getId().getCompetencyId().equals(id))
                                    .map(compTag -> compTag.getCompTag().getTag()).toList()
                    ));

            competencyAssignmentOverviewDtoList.add(new CompetencyAssignmentOverviewDto(
                    roleDto.getOrgChart().getId(),
                    roleDto.getOrgChart().getName(),
                    roleDto.getOrgChart().isDeleted(),
                    roleDto.getId(),
                    roleDto.getName(),
                    totalWeightage,
                    assignedCompetency.values().stream().toList(),
                    assignedCompTags
            ));
        });

        return competencyAssignmentOverviewDtoList;
    }

    @Override
    public RoleDetailsDto getRoleDetails(Long roleId) {
        if (roleId == null) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{VIEW_ROLE_DETAILS}, Locale.getDefault()));
        }

        RoleDto selectedRoleDto = roleService.getAllById(roleId);

        List<JobScopeDto> jobScopeDtoList = roleJobScopeService.findAllByRoleId(roleId).stream()
                .map(RoleJobScopeDto::getJobScope).toList();

        Map<Long, RoleCompetencyDto> roleCompetencyDto = roleCompetencyService.findAllByRoleId(roleId).stream()
                .collect(Collectors.toMap(
                        dto -> dto.getId().getCompetencyId(),
                        Function.identity()
                ));

        int totalWeightage = roleCompetencyDto.values().stream().mapToInt(RoleCompetencyDto::getWeightage).sum();

        List<StaffDto> staffDto = staffService.findAllByRoleId(roleId).stream()
                .peek(dto -> dto.setPassword(null))
                .toList();

        List<CompetencyCompTagDto> competencyCompTags = competencyCompTagService
                .findAllByCompetencyIdIn(roleCompetencyDto.keySet());

        Map<Long, List<String>> assignedCompTags = roleCompetencyDto.keySet().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        id -> competencyCompTags.stream()
                                .filter(compTag -> compTag.getId().getCompetencyId().equals(id))
                                .map(compTag -> compTag.getCompTag().getTag())
                                .toList()
                ));

        return new RoleDetailsDto(
                selectedRoleDto.getOrgChart().getId(),
                selectedRoleDto.getOrgChart().getName(),
                selectedRoleDto.getId(),
                selectedRoleDto.getName(),
                selectedRoleDto.getDescription(),
                selectedRoleDto.isVisible(),
                jobScopeDtoList,
                roleCompetencyDto.values().stream().toList(),
                staffDto,
                totalWeightage,
                assignedCompTags
        );
    }

    @Override
    public List<RoleDetailsDto> getRoleDetailsOverview() {
        List<RoleDto> allRoles = roleService.getAllByDeletedIsFalse();
        List<RoleJobScopeDto> allJobScopes = roleJobScopeService.findAll();
        List<RoleCompetencyDto> allCompetencies = roleCompetencyService.findAll();
        List<StaffDto> allStaffs = staffService.findAllByIsDeletedIsFalse();
        List<CompetencyCompTagDto> allCompTags = competencyCompTagService.findAll();

        return allRoles.stream().map(dto -> {
            List<JobScopeDto> assignedJobScopes = allJobScopes.stream()
                    .filter(jobScope -> Objects.equals(jobScope.getId().getRoleId(), dto.getId()))
                    .map(RoleJobScopeDto::getJobScope)
                    .collect(Collectors.toList());

            Map<Long, RoleCompetencyDto> assignedCompetency = allCompetencies.stream()
                    .filter(competency -> Objects.equals(competency.getId().getRoleId(), dto.getId()))
                    .collect(Collectors.toMap(
                            competency -> competency.getId().getCompetencyId(),
                            Function.identity()
                    ));

            List<StaffDto> assignedStaff = allStaffs.stream()
                    .filter(staff -> staff.getRole() != null && Objects.equals(staff.getRole().getId(), dto.getId()))
                    .toList();

            int totalWeightage = assignedCompetency.values().stream().mapToInt(RoleCompetencyDto::getWeightage).sum();

            List<CompetencyCompTagDto> assignedCompetencyCompTagDto = allCompTags.stream()
                    .filter(compTag -> assignedCompetency.containsKey(compTag.getId().getCompetencyId()))
                    .toList();

            Map<Long, List<String>> assignedCompTags = assignedCompetency.keySet().stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            id -> assignedCompetencyCompTagDto.stream()
                                    .filter(compTag -> compTag.getId().getCompetencyId().equals(id))
                                    .map(compTag -> compTag.getCompTag().getTag()).toList()
                    ));

            return new RoleDetailsDto(
                    dto.getOrgChart().getId(),
                    dto.getOrgChart().getName(),
                    dto.getId(),
                    dto.getName(),
                    dto.getDescription(),
                    dto.isVisible(),
                    assignedJobScopes,
                    assignedCompetency.values().stream().toList(),
                    assignedStaff,
                    totalWeightage,
                    assignedCompTags
            );
        }).toList();
    }

    @Override
    @Transactional
    public void createRoleCompetency(CreateRoleCompetencyRequestDto requestDto, UUID userId) {
        if (requestDto == null || requestDto.getRoleId() == null || requestDto.getCompetencyAssignment().isEmpty()) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{COMPETENCY_ASSIGNMENT_OPERATION}, Locale.getDefault()));
        }

        for (CompetencyAssignmentDto assignment : requestDto.getCompetencyAssignment()) {
            if (assignment.getWeightage() < WEIGHTAGE_MIN || assignment.getWeightage() > WEIGHTAGE_MAX) {
                throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{COMPETENCY_ASSIGNMENT_OPERATION}, Locale.getDefault()));
            }
        }

        RoleDto roleDto = roleService.getAllById(requestDto.getRoleId());

        List<RoleCompetencyDto> existingRoleCompetencyDtoList = roleCompetencyService.findAllByRoleId(requestDto.getRoleId());
        Set<Long> existingAssignedCompetencyIds = existingRoleCompetencyDtoList.stream()
                .map(roleCompetencyDto -> roleCompetencyDto.getId().getCompetencyId()).collect(Collectors.toSet());

        Map<Long, Integer> selectedCompetencyList = requestDto.getCompetencyAssignment().stream()
                .collect(Collectors.toMap(
                        CompetencyAssignmentDto::getCompetencyId,
                        CompetencyAssignmentDto::getWeightage,
                        (w1, w2) -> w2
                ));

        Set<Long> selectedCompetencyIds = selectedCompetencyList.keySet();

        Set<Long> toRemove = new HashSet<>(existingAssignedCompetencyIds);
        toRemove.removeAll(selectedCompetencyIds);

        if (!toRemove.isEmpty()) {
            Set<RoleCompetencyId> idToRemove = toRemove.stream()
                    .map(competencyId -> new RoleCompetencyId(requestDto.getRoleId(), competencyId))
                    .collect(Collectors.toSet());
            roleCompetencyService.deleteAllByIdIn(idToRemove);
        }

        Set<Long> toAdd = new HashSet<>(selectedCompetencyIds);
        toAdd.removeAll(existingAssignedCompetencyIds);

        OffsetDateTime now = OffsetDateTime.now();

        if (!toAdd.isEmpty()) {
            List<CompetencyDto> competencyDtoToAdd = competencyService.findAllByIsDeletedIsFalseAndIdIn(toAdd);

            List<RoleCompetencyDto> roleCompetencyDtoToAdd = competencyDtoToAdd.stream().map(competencyDto -> {
                RoleCompetencyDto roleCompetencyDto = new RoleCompetencyDto();
                roleCompetencyDto.setId(new RoleCompetencyId(requestDto.getRoleId(), competencyDto.getId()));
                roleCompetencyDto.setRole(roleDto);
                roleCompetencyDto.setCompetency(competencyDto);
                roleCompetencyDto.setWeightage(selectedCompetencyList.get(competencyDto.getId()));
                roleCompetencyDto.setCreatedBy(userId);
                roleCompetencyDto.setUpdatedBy(userId);
                roleCompetencyDto.setCreatedAt(now);
                roleCompetencyDto.setUpdatedAt(now);
                return roleCompetencyDto;
            }).toList();

            roleCompetencyService.createAll(roleCompetencyDtoToAdd);
        }

        Set<Long> toCheckWeightage = new HashSet<>(existingAssignedCompetencyIds);
        toCheckWeightage.retainAll(selectedCompetencyIds);

        if (!toCheckWeightage.isEmpty()) {
            Map<Long, RoleCompetencyDto> roleCompetencyDtoToCheckMap = existingRoleCompetencyDtoList.stream()
                    .filter(roleCompetencyDto -> toCheckWeightage.contains(roleCompetencyDto.getId().getCompetencyId()))
                    .collect(Collectors.toMap(
                            roleCompetencyDto -> roleCompetencyDto.getId().getCompetencyId(),
                            roleCompetencyDto -> roleCompetencyDto
                    ));

            toCheckWeightage.removeIf(competencyId -> Objects.equals(
                    selectedCompetencyList.get(competencyId),
                    roleCompetencyDtoToCheckMap.get(competencyId).getWeightage()
            ));

            List<RoleCompetencyDto> roleCompetencyDtosToBeUpdated = toCheckWeightage.stream().map(competencyId -> {
                RoleCompetencyDto roleCompetencyDto = roleCompetencyDtoToCheckMap.get(competencyId);
                roleCompetencyDto.setWeightage(selectedCompetencyList.get(competencyId));
                roleCompetencyDto.setUpdatedBy(userId);
                roleCompetencyDto.setUpdatedAt(now);
                return roleCompetencyDto;
            }).toList();

            Set<RoleCompetencyId> roleCompetencyIdsToBeUpdated = roleCompetencyDtosToBeUpdated.stream()
                    .map(RoleCompetencyDto::getId).collect(Collectors.toSet());

            if (!roleCompetencyIdsToBeUpdated.isEmpty()) {
                roleCompetencyService.updateAll(roleCompetencyIdsToBeUpdated, roleCompetencyDtosToBeUpdated);
            }
        }
    }

    @Override
    @Transactional
    public void deleteRoleCompetency(Long roleId) {
        if (roleId == null) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{COMPETENCY_ASSIGNMENT_DELETE_OPERATION}, Locale.getDefault()));
        }
        roleCompetencyService.deleteAllByRoleId(roleId);
    }

    @Override
    @Transactional
    public void bulkDeleteRoleCompetency(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{COMPETENCY_ASSIGNMENT_DELETE_OPERATION}, Locale.getDefault()));
        }
        roleCompetencyService.deleteAllByRoleIdIn(new HashSet<>(roleIds));
    }

    @Override
    public byte[] exportData() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Sheet 1");
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            int rowIndex = 0;

            Row header = sheet.createRow(rowIndex);

            Cell cellDeptName = header.createCell(0);
            cellDeptName.setCellValue(DEPT_NAME_COLUMN);
            createCellComment(drawing, cellDeptName, messageSource.getMessage(DEPT_NAME_NOTES, null, Locale.getDefault()));

            Cell cellRoleName = header.createCell(1);
            cellRoleName.setCellValue(ROLE_NAME_COLUMN);
            createCellComment(drawing, cellRoleName, messageSource.getMessage(ROLE_NAME_NOTES, null, Locale.getDefault()));

            Cell cellComp = header.createCell(2);
            cellComp.setCellValue(COMPETENCY_WEIGHTAGE_COLUMN);
            createCellComment(drawing, cellComp, messageSource.getMessage(COMPETENCY_NAME_NOTES, null, Locale.getDefault()));

            rowIndex++;

            List<RoleDto> allRoles = roleService.getAllByDeletedIsFalse();
            List<RoleCompetencyDto> allAssignedCompetency = roleCompetencyService.findAll();

            for (RoleDto dto : allRoles) {
                String assignedCompetency = allAssignedCompetency.stream()
                        .filter(c -> Objects.equals(c.getRole().getId(), dto.getId()))
                        .map(c -> String.format("%s > %d", c.getCompetency().getName(), c.getWeightage()))
                        .collect(Collectors.joining("; "));

                Row row = sheet.createRow(rowIndex);
                row.createCell(0).setCellValue(dto.getOrgChart().getName());
                row.createCell(1).setCellValue(dto.getName());
                row.createCell(2).setCellValue(assignedCompetency);

                rowIndex++;
            }

            workbook.write(out);
            return out.toByteArray();
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
        Map<String, Set<String>> alldepartmentRoleNameMap = allDepartment.stream().collect(Collectors.toMap(
                dto -> dto.getName().trim().toLowerCase(),
                dto -> allRoles.stream().filter(role ->
                                Objects.equals(role.getOrgChart().getId(), dto.getId()))
                        .map(role -> role.getName().toLowerCase().trim())
                        .collect(Collectors.toSet())));

        List<CompetencyDto> allCompetency = competencyService.findAllByIsDeletedIsFalse();
        Map<String, CompetencyDto> allCompetencyNameMap = allCompetency.stream()
                .collect(Collectors.toMap(dto -> dto.getName().trim().toLowerCase(), Function.identity()));
        Map<Long, CompetencyDto> allCompetencyIdMap = allCompetency.stream()
                .collect(Collectors.toMap(CompetencyDto::getId, Function.identity()));

        List<RoleCompetencyDto> allAssignedCompetency = roleCompetencyService.findAll();
        Map<RoleCompetencyId, RoleCompetencyDto> allAssignedCompetencyMap = allAssignedCompetency.stream()
                .collect(Collectors.toMap(RoleCompetencyDto::getId, Function.identity()));

        Sheet sheet = workbook.getSheetAt(0);

        Row header = sheet.getRow(0);
        if (!getCellValueAsString(header.getCell(0)).equalsIgnoreCase(DEPT_NAME_COLUMN)
                || !getCellValueAsString(header.getCell(1)).equalsIgnoreCase(ROLE_NAME_COLUMN)
                || !getCellValueAsString(header.getCell(2)).equalsIgnoreCase(COMPETENCY_WEIGHTAGE_COLUMN)) {
            throw new BadRequestException(messageSource.getMessage(IMPORT_FILE_INVALID_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        List<RoleCompetencyDto> roleCompetencyToBeCreated = new ArrayList<>();
        Set<RoleCompetencyId> roleCompetencyIdsToRemove = new HashSet<>();
        Map<String, Set<String>> duplicatedRow = new HashMap<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || row.getCell(0) == null || getCellValueAsString(row.getCell(0)) == null
                    || getCellValueAsString(row.getCell(0)).isBlank()) continue;

            String departmentName = getCellValueAsString(row.getCell(0));
            String roleName = getCellValueAsString(row.getCell(1));
            String competencies = getCellValueAsString(row.getCell(2));

            Map<Long, Integer> competencyWeightageMap;
            if (!validationService.isNullOrBlank(competencies)) {
                int finalI = i;
                competencyWeightageMap = Arrays.stream(competencies.split(";"))
                        .map(String::trim)
                        .filter(competencyWeightage -> !validationService.isNullOrBlank(competencyWeightage))
                        .peek(competencyWeightage -> {
                            String[] parts = competencyWeightage.split(">");
                            if (parts.length != 2) {
                                throw new BadRequestException(messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE,
                                        new String[]{COMPETENCY_WEIGHTAGE_COLUMN, Integer.toString(finalI + 1)}, Locale.getDefault()));
                            }

                            String competencyName = parts[0].trim();
                            String weightage = parts[1].trim();

                            if (!allCompetencyNameMap.containsKey(competencyName.toLowerCase())) {
                                throw new BadRequestException(messageSource.getMessage(IMPORT_DATA_NOT_FOUND_ERR_MSG_CODE,
                                        new String[]{competencyName}, Locale.getDefault()));
                            }

                            if (!weightage.matches("^\\d{1,3}$")) {
                                throw new BadRequestException(messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE,
                                        new String[]{WEIGHTAGE_COLUMN, Integer.toString(finalI + 1)}, Locale.getDefault()));
                            }

                            int weightValue = Integer.parseInt(weightage);
                            if (weightValue < WEIGHTAGE_MIN || weightValue > WEIGHTAGE_MAX) {
                                throw new BadRequestException(messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE,
                                        new String[]{WEIGHTAGE_COLUMN, Integer.toString(finalI + 1)}, Locale.getDefault()));
                            }
                        })
                        .collect(Collectors.toMap(
                                cw -> Objects.requireNonNull(allCompetencyNameMap.get(cw.split(">")[0].toLowerCase().trim())).getId(),
                                cw -> Integer.parseInt(cw.split(">")[1].trim())
                        ));
            } else {
                competencyWeightageMap = Collections.emptyMap();
            }

            if (validationService.isNullOrBlank(departmentName)) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE,
                        new String[]{DEPT_NAME_COLUMN, Integer.toString(i + 1)}, Locale.getDefault()));
            }

            if (validationService.isNullOrBlank(roleName)) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE,
                        new String[]{ROLE_NAME_COLUMN, Integer.toString(i + 1)}, Locale.getDefault()));
            }

            if (!alldepartmentRoleNameMap.containsKey(departmentName.trim().toLowerCase())) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_DATA_NOT_FOUND_ERR_MSG_CODE,
                        new String[]{departmentName}, Locale.getDefault()));
            }

            if (!alldepartmentRoleNameMap.get(departmentName.trim().toLowerCase()).contains(roleName.trim().toLowerCase())) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_DATA_NOT_FOUND_ERR_MSG_CODE,
                        new String[]{roleName}, Locale.getDefault()));
            }

            if (duplicatedRow.get(departmentName.toLowerCase().trim()) != null
                    && duplicatedRow.get(departmentName.toLowerCase().trim()).contains(roleName.toLowerCase().trim())) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_DUPLICATE_ENTRY_ERR_MSG_CODE,
                        new String[]{Integer.toString(i + 1)}, Locale.getDefault()));
            }

            duplicatedRow.computeIfAbsent(departmentName.toLowerCase().trim(), k -> new HashSet<>())
                    .add(roleName.toLowerCase().trim());

            RoleDto selectedRoleDto = Objects.requireNonNull(
                    allRoles.stream().filter(dto -> roleName.trim().equalsIgnoreCase(dto.getName().trim()))
                            .findFirst().orElse(null));

            if (!competencyWeightageMap.isEmpty()) {
                Map<Long, Integer> assignedWeightageMap = allAssignedCompetency.stream()
                        .filter(dto -> Objects.equals(dto.getId().getRoleId(), selectedRoleDto.getId()))
                        .collect(Collectors.toMap(
                                dto -> dto.getId().getCompetencyId(),
                                RoleCompetencyDto::getWeightage
                        ));

                Set<Long> selectedCompetencyId = competencyWeightageMap.keySet();

                Set<Long> toAdd = new HashSet<>(selectedCompetencyId);
                toAdd.removeAll(assignedWeightageMap.keySet());

                Set<Long> toRemove = new HashSet<>(assignedWeightageMap.keySet());
                toRemove.removeAll(selectedCompetencyId);

                Set<Long> toCheck = new HashSet<>(selectedCompetencyId);
                toCheck.retainAll(assignedWeightageMap.keySet());

                if (!toRemove.isEmpty()) {
                    roleCompetencyIdsToRemove.addAll(
                            toRemove.stream().map(competencyId -> new RoleCompetencyId(selectedRoleDto.getId(), competencyId))
                                    .collect(Collectors.toSet())
                    );
                }

                if (!toCheck.isEmpty()) {
                    List<RoleCompetencyDto> toUpdate = toCheck.stream()
                            .map(competencyId -> {
                                RoleCompetencyDto assignedCompetency = allAssignedCompetencyMap.get(
                                        new RoleCompetencyId(selectedRoleDto.getId(), competencyId));
                                Integer newWeightage = competencyWeightageMap.get(competencyId);
                                Integer oldWeightage = assignedCompetency.getWeightage();
                                if (!Objects.equals(oldWeightage, newWeightage)) {
                                    assignedCompetency.setWeightage(newWeightage);
                                    assignedCompetency.setUpdatedAt(now);
                                    assignedCompetency.setCreatedBy(userId);
                                    return assignedCompetency;
                                }
                                return null;
                            })
                            .filter(Objects::nonNull)
                            .toList();

                    roleCompetencyToBeCreated.addAll(toUpdate);
                }

                if (!toAdd.isEmpty()) {
                    List<RoleCompetencyDto> toCreate = toAdd.stream().map(competencyId ->
                                    new RoleCompetencyDto(
                                            new RoleCompetencyId(selectedRoleDto.getId(), competencyId),
                                            selectedRoleDto,
                                            allCompetencyIdMap.get(competencyId),
                                            competencyWeightageMap.get(competencyId),
                                            userId,
                                            now,
                                            userId,
                                            now
                                    ))
                            .toList();

                    roleCompetencyToBeCreated.addAll(toCreate);
                }
            }
        }

        Set<RoleCompetencyId> roleCompetencyIdToBeCreated = roleCompetencyToBeCreated.stream()
                .map(RoleCompetencyDto::getId).collect(Collectors.toSet());

        if (!roleCompetencyIdToBeCreated.isEmpty()) {
            roleCompetencyService.updateAll(roleCompetencyIdToBeCreated, roleCompetencyToBeCreated);
        }

        if (!roleCompetencyIdsToRemove.isEmpty()) {
            roleCompetencyService.deleteAllByIdIn(roleCompetencyIdsToRemove);
        }
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
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else {
            return cell.getStringCellValue().trim();
        }
    }
}
