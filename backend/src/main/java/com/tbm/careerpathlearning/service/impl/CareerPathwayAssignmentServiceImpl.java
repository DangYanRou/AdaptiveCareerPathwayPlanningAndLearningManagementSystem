package com.tbm.careerpathlearning.service.impl;

import com.tbm.careerpathlearning.dto.*;
import com.tbm.careerpathlearning.service.*;
import com.tbm.careerpathlearning.exception.BadRequestException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CareerPathwayAssignmentServiceImpl implements CareerPathwayAssignmentService {

    @Autowired
    private CareerPathwayService careerPathwayService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private StaffService staffService;

    @Autowired
    private CareerPathwayRoleService careerPathwayRoleService;

    @Autowired
    private OrgChartService orgChartService;

    private static final String INVALID_REQUEST_BODY_ERR_MSG_CODE = "invalid.request.body.err.msg";
    private static final String CAREER_PATHWAY_OUTSIDE_ERR_MSG_CODE = "career.pathway.outside.err.msg";
    private static final String IMPORT_CAREER_PATHWAY_OUTSIDE_ERR_MSG_CODE = "import.career.pathway.outside.err.msg";
    private static final String CAREER_PATHWAY_ASSIGNMENT = "Assign Career Pathway";
    private static final String CAREER_PATHWAY_UPDATE = "Update Career Pathway Assignment";
    private static final String CAREER_PATHWAY_DELETE = "Remove Career Pathway Assignment";
    private static final String DEPT_NAME_NOTES = "career.pathway.assignment.department.name.notes";
    private static final String COMPETENCY_NAME_NOTES = "career.pathway.assignment.career.pathway.name.notes";
    private static final String STAFF_EMAIL_NOTES = "career.pathway.assignment.staff.email.notes";
    private static final String DEPT_NAME_COLUMN = "Department Name";
    private static final String CAREER_PATHWAY_NAME_COLUMN = "Career Pathway Name";
    private static final String STAFF_EMAIL_COLUMN = "Staff Email";
    private static final String IMPORT_OPERATION = "Import Role Assignment Data";
    private static final String IMPORT_INVALID_DATA_ERR_MSG_CODE = "import.invalid.data.err.msg";
    private static final String IMPORT_FILE_TOO_LARGE_ERR_MSG_CODE = "import.file.too.large.err.msg";
    private static final String IMPORT_FILE_INVALID_ERR_MSG_CODE = "import.file.invalid.err.msg";
    private static final String IMPORT_DUPLICATE_ENTRY_ERR_MSG_CODE = "import.duplicate.entry.err.msg";
    private static final String IMPORT_DATA_NOT_FOUND_ERR_MSG_CODE = "import.data.not.found.err.msg";
    private static final int MAX_FILE_SIZE_IN_MB = 5;
    private static final String ACCEPTED_IMPORT_FILE_TYPE = ".xlsx";

    @Override
    public List<CareerPathwayAssignmentOverviewDto> getOverview() {
        List<CareerPathwayDto> existedCareerPathway = careerPathwayService.getAllByIsDeletedIsFalse();
        List<StaffDto> existedStaffs = staffService.findAllByIsDeletedIsFalse();

        return existedCareerPathway.stream().map(dto -> {
            List<StaffDto> assignedStaffs = existedStaffs.stream()
                    .filter(staffDto -> staffDto.getCareerPathway() != null
                    && Objects.equals(staffDto.getCareerPathway().getId(), dto.getId()))
                    .peek(staffDto -> staffDto.setPassword(null))
                    .toList();

            return new CareerPathwayAssignmentOverviewDto(
                    dto.getOrgChart().getId(),
                    dto.getOrgChart().getName(),
                    dto.getOrgChart().isDeleted(),
                    dto.getId(),
                    dto.getName(),
                    assignedStaffs
            );
        }).toList();
    }

    @Override
    @Transactional
    public void assign(AssignCareerPathwayRequestDto req, UUID userId) {
        if (req == null || req.getCareerPathwayId() == null || req.getStaffIds() == null || req.getStaffIds().isEmpty()) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{CAREER_PATHWAY_ASSIGNMENT}, Locale.getDefault()));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(8));

        CareerPathwayDto selectedCareerPathway = careerPathwayService.getById(req.getCareerPathwayId());
        List<CareerPathwayRoleDto> relation = careerPathwayRoleService.getAllByCareerPathwayId(req.getCareerPathwayId());
        Set<RoleDto> assignedRoles = relation.stream().map(CareerPathwayRoleDto::getParentRole).collect(Collectors.toSet());
        assignedRoles.addAll(relation.stream().map(CareerPathwayRoleDto::getChildRole).collect(Collectors.toSet()));
        assignedRoles.add(selectedCareerPathway.getRootRole());

        Set<Long> assignedRoleIds = assignedRoles.stream().map(RoleDto::getId).collect(Collectors.toSet());

        List<StaffDto> selectedStaffs = staffService.findAllByIdIn(req.getStaffIds());
        Set<UUID> selectedStaffUUID = selectedStaffs.stream().map(StaffDto::getId).collect(Collectors.toSet());
        Set<Long> selectedRoleIds = selectedStaffs.stream()
                .filter(dto -> dto.getRole() != null)
                .map(dto -> dto.getRole().getId()).collect(Collectors.toSet());

        if (!assignedRoleIds.containsAll(selectedRoleIds)) {
            throw new BadRequestException(messageSource.getMessage(CAREER_PATHWAY_OUTSIDE_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        staffService.updateCareerPathwayByIdIn(selectedStaffUUID, selectedCareerPathway, userId, now);
    }

    @Override
    @Transactional
    public void update(AssignCareerPathwayRequestDto req, UUID userId) {
        if (req == null || req.getCareerPathwayId() == null) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{CAREER_PATHWAY_UPDATE}, Locale.getDefault()));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(8));

        CareerPathwayDto selectedCareerPathway = careerPathwayService.getById(req.getCareerPathwayId());
        List<CareerPathwayRoleDto> relation = careerPathwayRoleService.getAllByCareerPathwayId(req.getCareerPathwayId());
        Set<RoleDto> assignedRoles = relation.stream().map(CareerPathwayRoleDto::getParentRole).collect(Collectors.toSet());
        assignedRoles.addAll(relation.stream().map(CareerPathwayRoleDto::getChildRole).collect(Collectors.toSet()));
        assignedRoles.add(selectedCareerPathway.getRootRole());

        Set<Long> assignedRoleIds = assignedRoles.stream().map(RoleDto::getId).collect(Collectors.toSet());

        List<StaffDto> selectedStaffs = staffService.findAllByIdIn(req.getStaffIds());
        Set<UUID> selectedStaffUUID = selectedStaffs.stream().map(StaffDto::getId).collect(Collectors.toSet());
        Set<Long> selectedRoleIds = selectedStaffs.stream()
                .filter(dto -> dto.getRole() != null)
                .map(dto -> dto.getRole().getId()).collect(Collectors.toSet());

        Set<UUID> assignedStaffUUID = staffService.findAllByCareerPathwayId(req.getCareerPathwayId())
                .stream().map(StaffDto::getId).collect(Collectors.toSet());

        if (!assignedRoleIds.containsAll(selectedRoleIds)) {
            throw new BadRequestException(messageSource.getMessage(CAREER_PATHWAY_OUTSIDE_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        Set<UUID> toAdd = new HashSet<>(selectedStaffUUID);
        toAdd.removeAll(assignedStaffUUID);

        Set<UUID> toRemove = new HashSet<>(assignedStaffUUID);
        toRemove.removeAll(selectedStaffUUID);

        if (!toAdd.isEmpty()) {
            staffService.updateCareerPathwayByIdIn(toAdd, selectedCareerPathway, userId, now);
        }

        if (!toRemove.isEmpty()) {
            staffService.updateCareerPathwayByIdIn(toRemove, null, userId, now);
        }
    }

    @Override
    @Transactional
    public void delete(Long careerPathwayId, UUID userId) {
        if (careerPathwayId == null) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{CAREER_PATHWAY_DELETE}, Locale.getDefault()));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(8));

        Set<UUID> assignedStaffUUID = staffService.findAllByCareerPathwayId(careerPathwayId)
                .stream().map(StaffDto::getId).collect(Collectors.toSet());

        staffService.updateCareerPathwayByIdIn(assignedStaffUUID, null, userId, now);
    }

    @Override
    @Transactional
    public void bulkDelete(Set<Long> careerPathwayIds, UUID userId) {
        if (careerPathwayIds == null || careerPathwayIds.isEmpty()) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{CAREER_PATHWAY_DELETE}, Locale.getDefault()));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(8));

        Set<UUID> assignedStaffUUID = staffService.findAllByCareerPathwayIdIn(careerPathwayIds)
                .stream().map(StaffDto::getId).collect(Collectors.toSet());

        staffService.updateCareerPathwayByIdIn(assignedStaffUUID, null, userId, now);
    }

    @Override
    public ByteArrayResource exportData() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Sheet 1");
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            int rowIndex = 0;

            Row header = sheet.createRow(rowIndex);

            Cell cellDeptName = header.createCell(0);
            cellDeptName.setCellValue(DEPT_NAME_COLUMN);
            createCellComment(drawing, cellDeptName, messageSource.getMessage(DEPT_NAME_NOTES, null, Locale.getDefault()));

            Cell cellCareerPathwayName = header.createCell(1);
            cellCareerPathwayName.setCellValue(CAREER_PATHWAY_NAME_COLUMN);
            createCellComment(drawing, cellCareerPathwayName, messageSource.getMessage(COMPETENCY_NAME_NOTES, null, Locale.getDefault()));

            Cell cellStaffEmail = header.createCell(2);
            cellStaffEmail.setCellValue(STAFF_EMAIL_COLUMN);
            createCellComment(drawing, cellStaffEmail, messageSource.getMessage(STAFF_EMAIL_NOTES, null, Locale.getDefault()));

            rowIndex++;

            List<CareerPathwayDto> allCareerPathway = careerPathwayService.getAllByIsDeletedIsFalse();
            List<StaffDto> allStaff = staffService.findAllByIsDeletedIsFalse();

            for (CareerPathwayDto dto : allCareerPathway) {
                String assignedStaff = allStaff.stream()
                        .filter(staff -> staff.getCareerPathway() != null && Objects.equals(staff.getCareerPathway().getId(), dto.getId()))
                        .map(StaffDto::getEmail)
                        .collect(Collectors.joining("; "));

                Row row = sheet.createRow(rowIndex);
                row.createCell(0).setCellValue(dto.getOrgChart().getName());
                row.createCell(1).setCellValue(dto.getName());
                row.createCell(2).setCellValue(assignedStaff);

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
        List<CareerPathwayDto> allCareerPathway = careerPathwayService.getAllByIsDeletedIsFalse();
        Map<String, Set<String>> allDepartmentCareerPathwayNameMap = allDepartment.stream().collect(Collectors.toMap(
                dto -> dto.getName().trim().toLowerCase(),
                dto -> allCareerPathway.stream()
                        .filter(careerPathway -> Objects.equals(careerPathway.getOrgChart().getId(), dto.getId()))
                        .map(careerPathway -> careerPathway.getName().toLowerCase().trim())
                        .collect(Collectors.toSet())));

        Map<String, StaffDto> allEmailStaffMap = staffService.findAllByIsDeletedIsFalse().stream()
                .collect(Collectors.toMap(StaffDto::getEmail, Function.identity()));

        List<CareerPathwayRoleDto> allRelation = careerPathwayRoleService.getAll();

        Map<Long, Set<String>> allCareerPathwayStaffMap = new HashMap<>();
        allEmailStaffMap.values().forEach(staffMap -> {
            if (staffMap.getCareerPathway() != null) {
                allCareerPathwayStaffMap.computeIfAbsent(staffMap.getCareerPathway().getId(), k -> new HashSet<>())
                        .add(staffMap.getEmail().toLowerCase().trim());
            }
        });

        Sheet sheet = workbook.getSheetAt(0);

        Row header = sheet.getRow(0);
        if (!getCellValueAsString(header.getCell(0)).equalsIgnoreCase(DEPT_NAME_COLUMN)
                || !getCellValueAsString(header.getCell(1)).equalsIgnoreCase(CAREER_PATHWAY_NAME_COLUMN)
                || !getCellValueAsString(header.getCell(2)).equalsIgnoreCase(STAFF_EMAIL_COLUMN)) {
            throw new BadRequestException(messageSource.getMessage(IMPORT_FILE_INVALID_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        Map<UUID, CareerPathwayDto> pendingRoleAssignments = new HashMap<>();
        Map<String, Set<String>> duplicatedRow = new HashMap<>();
        Set<String> emailSeen = new HashSet<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || row.getCell(0) == null || getCellValueAsString(row.getCell(0)) == null
                    || getCellValueAsString(row.getCell(0)).isBlank()) {
                continue;
            }

            String departmentName = getCellValueAsString(row.getCell(0));
            String careerPathwayName = getCellValueAsString(row.getCell(1));
            String staffEmails = getCellValueAsString(row.getCell(2));

            Set<String> emails;
            if (!validationService.isNullOrBlank(staffEmails)) {
                int finalI = i;
                emails = Arrays.stream(staffEmails.split(";"))
                        .map(String::trim)
                        .filter(email -> !validationService.isNullOrBlank(email))
                        .peek(email -> {
                            if (email.length() > 255 || !allEmailStaffMap.containsKey(email)) {
                                throw new BadRequestException(messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE,
                                        new String[]{STAFF_EMAIL_COLUMN, Integer.toString(finalI + 1)}, Locale.getDefault()));
                            }
                            if (emailSeen.contains(email)) {
                                throw new BadRequestException(messageSource.getMessage(IMPORT_DUPLICATE_ENTRY_ERR_MSG_CODE,
                                        new String[]{Integer.toString(finalI + 1)}, Locale.getDefault()));
                            }
                            emailSeen.add(email);
                        })
                        .collect(Collectors.toSet());
            } else {
                emails = Collections.emptySet();
            }

            if (validationService.isNullOrBlank(departmentName)) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE,
                        new String[]{DEPT_NAME_COLUMN, Integer.toString(i + 1)}, Locale.getDefault()));
            }

            if (validationService.isNullOrBlank(careerPathwayName)) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE,
                        new String[]{CAREER_PATHWAY_NAME_COLUMN, Integer.toString(i + 1)}, Locale.getDefault()));
            }

            if (!allDepartmentCareerPathwayNameMap.containsKey(departmentName.trim().toLowerCase())) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_DATA_NOT_FOUND_ERR_MSG_CODE, new String[]{departmentName}, Locale.getDefault()));
            }

            if (!allDepartmentCareerPathwayNameMap.get(departmentName.trim().toLowerCase()).contains(careerPathwayName.trim().toLowerCase())) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_DATA_NOT_FOUND_ERR_MSG_CODE, new String[]{careerPathwayName}, Locale.getDefault()));
            }

            if (duplicatedRow.get(departmentName.toLowerCase().trim()) != null
                    && duplicatedRow.get(departmentName.toLowerCase().trim()).contains(careerPathwayName.toLowerCase().trim())) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_DUPLICATE_ENTRY_ERR_MSG_CODE, new String[]{Integer.toString(i + 1)}, Locale.getDefault()));
            }

            duplicatedRow.computeIfAbsent(departmentName.toLowerCase().trim(), k -> new HashSet<>())
                    .add(careerPathwayName.toLowerCase().trim());

            CareerPathwayDto selectedCareerPathwayDto = Objects.requireNonNull(
                    allCareerPathway.stream().filter(dto
                            -> careerPathwayName.trim().equalsIgnoreCase(dto.getName().trim())
                    && departmentName.trim().equalsIgnoreCase(dto.getOrgChart().getName().trim()))
                            .findFirst().orElse(null));

            List<CareerPathwayRoleDto> assignedRole = allRelation.stream()
                    .filter(relation -> Objects.equals(relation.getId().getCareerPathwayId(), selectedCareerPathwayDto.getId()))
                    .toList();

            Set<Long> assignedRoleIds = new HashSet<>();
            assignedRoleIds.add(selectedCareerPathwayDto.getRootRole().getId());
            assignedRoleIds.addAll(assignedRole.stream().map(role -> role.getId().getParentId()).collect(Collectors.toSet()));
            assignedRoleIds.addAll(assignedRole.stream().map(role -> role.getId().getChildId()).collect(Collectors.toSet()));

            int finalI = i;

            Set<String> assignedStaffs = allCareerPathwayStaffMap.getOrDefault(selectedCareerPathwayDto.getId(), new HashSet<>());

            Set<String> toAdd = new HashSet<>(emails);
            toAdd.removeAll(assignedStaffs);

            Set<String> toRemove = new HashSet<>(assignedStaffs);
            toRemove.removeAll(emails);

            if (!toAdd.isEmpty()) {
                List<StaffDto> staffToAdd = allEmailStaffMap.entrySet().stream()
                        .filter(entry -> toAdd.contains(entry.getKey()))
                        .map(entry -> {
                            if (entry.getValue().getRole() == null || !assignedRoleIds.contains(entry.getValue().getRole().getId())) {
                                throw new BadRequestException(messageSource.getMessage(IMPORT_CAREER_PATHWAY_OUTSIDE_ERR_MSG_CODE,
                                        new String[]{Integer.toString(finalI + 1)}, Locale.getDefault()));
                            }
                            return entry.getValue();
                        })
                        .toList();

                staffToAdd.forEach(staff -> pendingRoleAssignments.put(staff.getId(), selectedCareerPathwayDto));
            }

            if (!toRemove.isEmpty()) {
                List<StaffDto> staffToRemove = allEmailStaffMap.entrySet().stream()
                        .filter(entry -> toRemove.contains(entry.getKey()))
                        .map(Map.Entry::getValue)
                        .toList();

                staffToRemove.forEach(staff -> pendingRoleAssignments.putIfAbsent(staff.getId(), null));
            }
        }

        List<StaffDto> finalStaffToUpdate = new ArrayList<>();

        for (Map.Entry<UUID, CareerPathwayDto> entry : pendingRoleAssignments.entrySet()) {
            UUID staffId = entry.getKey();
            CareerPathwayDto newCareerPathway = entry.getValue();

            StaffDto dto = allEmailStaffMap.values().stream()
                    .filter(s -> s.getId().equals(staffId))
                    .findFirst()
                    .orElse(null);

            if (dto != null) {
                boolean isRoleChanging = (dto.getCareerPathway() == null && newCareerPathway != null)
                        || (dto.getCareerPathway() != null && newCareerPathway == null)
                        || (dto.getCareerPathway() != null && newCareerPathway != null && !dto.getCareerPathway().getId().equals(newCareerPathway.getId()));

                if (isRoleChanging) {
                    dto.setCareerPathway(newCareerPathway);
                    dto.setUpdatedBy(userId);
                    dto.setUpdatedAt(now);
                    finalStaffToUpdate.add(dto);
                }
            }
        }

        if (!finalStaffToUpdate.isEmpty()) {
            staffService.updateAll(finalStaffToUpdate);
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
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else {
            return cell.getStringCellValue().trim();
        }
    }
}
