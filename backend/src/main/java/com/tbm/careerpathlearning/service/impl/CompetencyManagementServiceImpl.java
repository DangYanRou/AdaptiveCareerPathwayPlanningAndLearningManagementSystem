package com.tbm.careerpathlearning.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.apache.poi.ss.usermodel.CellType;
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

import com.tbm.careerpathlearning.dto.CompTagDto;
import com.tbm.careerpathlearning.dto.CompetencyCompTagDto;
import com.tbm.careerpathlearning.dto.CompetencyDto;
import com.tbm.careerpathlearning.dto.CompetencyOverviewDto;
import com.tbm.careerpathlearning.dto.CreateCompetencyRequestDto;
import com.tbm.careerpathlearning.dto.EditCompetencyRequestDto;
import com.tbm.careerpathlearning.exception.BadRequestException;
import com.tbm.careerpathlearning.model.CompetencyCompTagId;
import com.tbm.careerpathlearning.service.CompTagService;
import com.tbm.careerpathlearning.service.CompetencyCompTagService;
import com.tbm.careerpathlearning.service.CompetencyManagementService;
import com.tbm.careerpathlearning.service.CompetencyService;
import com.tbm.careerpathlearning.service.ValidationService;

@Service
public class CompetencyManagementServiceImpl implements CompetencyManagementService{

    @Autowired
    private CompetencyService competencyService;

    @Autowired
    private CompetencyCompTagService competencyCompTagService;

    @Autowired
    private CompTagService compTagService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private MessageSource messageSource;

    private static final String INVALID_REQUEST_BODY_ERR_MSG_CODE = "invalid.request.body.err.msg";

    private static final String COMPETENCY_CREATION_OPERATION = "Competency Creation";
    private static final String COMPETENCY_EDIT_OPERATION = "Update Competency";
    private static final String COMPETENCY_DELETE_OPERATION = "Competency Deletion";
    private static final String IMPORT_OPERATION = "Import Role Overview Data";

    private static final String IMPORT_INVALID_DATA_ERR_MSG_CODE = "import.invalid.data.err.msg";
    private static final String IMPORT_UNIQUE_NAME_ERR_MSG_CODE = "import.unique.name.err.msg";
    private static final String IMPORT_ACTION_CONFLICT_ERR_MSG_CODE = "import.action.conflict.err.msg";
    private static final String IMPORT_FILE_TOO_LARGE_ERR_MSG_CODE = "import.file.too.large.err.msg";
    private static final String IMPORT_FILE_INVALID_ERR_MSG_CODE = "import.file.invalid.err.msg";
    private static final String IMPORT_DUPLICATE_ENTRY_ERR_MSG_CODE = "import.duplicate.entry.err.msg";

    private static final String COMPETENCY_NAME_COLUMN = "Competency Name";
    private static final String COMPETENCY_DESC_COLUMN = "Competency Description";
    private static final String TAGS_COLUMN = "Tags";
    private static final String NEW_COMPETENCY_NAME_COLUMN = "New Competency Name";
    private static final String TO_BE_DELETED_COLUMN = "To Be Deleted";

    private static final String COMPETENCY_NAME_NOTES = "competency.definition.name.notes";
    private static final String COMPETENCY_DESC_NOTES = "competency.definition.desc.notes";
    private static final String TAGS_NOTES = "competency.definition.tags.notes";
    private static final String NEW_COMPETENCY_NAME_NOTES = "competency.definition.new.name.notes";
    private static final String TO_BE_DELETED_NOTES = "competency.definition.deleted.notes";

    private static final String YES = "Yes";
    private static final int MAX_FILE_SIZE_IN_MB = 5;
    private static final String ACCEPTED_IMPORT_FILE_TYPE = ".xlsx";

    @Override
    public List<CompetencyOverviewDto> getOverview() {
        List<CompetencyOverviewDto> competencyOverviewDtoList = new ArrayList<>();

        List<CompetencyDto> competencyDtoList = competencyService.findAllByIsDeletedIsFalse();
        List<CompetencyCompTagDto> competencyCompTagDtoList = competencyCompTagService.findAll();

        for (CompetencyDto competencyDto : competencyDtoList) {
            CompetencyOverviewDto competencyOverviewDto = new CompetencyOverviewDto();

            List<CompetencyCompTagDto> assignedCompetencyCompTagDtoList = competencyCompTagDtoList.stream()
                    .filter(competencyCompTagDto
                            -> Objects.equals(competencyCompTagDto.getCompetency().getId(), competencyDto.getId()))
                    .toList();

            List<CompTagDto> assignedCompTagDtoList = assignedCompetencyCompTagDtoList.stream()
                    .map(CompetencyCompTagDto::getCompTag).collect(Collectors.toList());

            competencyOverviewDto.setCompetencyId(competencyDto.getId());
            competencyOverviewDto.setCompetencyName(competencyDto.getName());
            competencyOverviewDto.setCompetencyDescription(competencyDto.getDescription());
            competencyOverviewDto.setAssignedCompTags(assignedCompTagDtoList);

            competencyOverviewDtoList.add(competencyOverviewDto);
        }

        return competencyOverviewDtoList;
    }

    @Override
    @Transactional
    public void create(CreateCompetencyRequestDto requestDto, UUID userId) {
        if (requestDto == null || requestDto.getCompetencyName() == null || validationService.isNullOrBlank(requestDto.getCompetencyName())) {
            String errorMessage = messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{COMPETENCY_CREATION_OPERATION}, Locale.getDefault());
            throw new BadRequestException(errorMessage);
        }

        CompetencyDto competencyDto = new CompetencyDto();
        competencyDto.setName(requestDto.getCompetencyName().trim());
        competencyDto.setDescription(requestDto.getCompetencyDescription().trim());
        competencyDto.setDeleted(false);
        competencyDto.setCreatedAt(OffsetDateTime.now());
        competencyDto.setCreatedBy(userId);
        competencyDto.setUpdatedAt(OffsetDateTime.now());
        competencyDto.setUpdatedBy(userId);

        CompetencyDto createdCompetency = competencyService.create(competencyDto);

        if (requestDto.getCompTagList() != null && !requestDto.getCompTagList().isEmpty()) {
            List<CompTagDto> compTagToBeCreated = new ArrayList<>();

            for (String tag : requestDto.getCompTagList()) {
                CompTagDto comptagDto = new CompTagDto();
                comptagDto.setTag(tag);
                comptagDto.setDeleted(false);
                comptagDto.setCreatedBy(userId);
                comptagDto.setCreatedAt(OffsetDateTime.now());
                comptagDto.setUpdatedBy(userId);
                comptagDto.setUpdatedAt(OffsetDateTime.now());
                compTagToBeCreated.add(comptagDto);
            }

            List<CompTagDto> createdCompTagDtoList = compTagService.createAll(compTagToBeCreated);

            List<CompetencyCompTagDto> competencyCompTagDtoList = new ArrayList<>();

            for (CompTagDto createdCompTagDto : createdCompTagDtoList) {
                CompetencyCompTagDto competencyCompTadDto = new CompetencyCompTagDto();
                competencyCompTadDto.setId(new CompetencyCompTagId(createdCompetency.getId(), createdCompTagDto.getId()));
                competencyCompTadDto.setCompetency(createdCompetency);
                competencyCompTadDto.setCompTag(createdCompTagDto);
                competencyCompTadDto.setCreatedBy(userId);
                competencyCompTadDto.setCreatedAt(OffsetDateTime.now());
                competencyCompTadDto.setUpdatedBy(userId);
                competencyCompTadDto.setUpdatedAt(OffsetDateTime.now());
                competencyCompTagDtoList.add(competencyCompTadDto);
            }

            competencyCompTagService.createAll(competencyCompTagDtoList);
        }
    }

    @Override
    @Transactional
    public void update(EditCompetencyRequestDto requestDto, UUID userId) {
        if (requestDto == null || requestDto.getCompetencyId() == null || requestDto.getCompetencyName() == null || requestDto.getCompetencyName().isBlank()) {
            String errorMessage = messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{COMPETENCY_EDIT_OPERATION}, Locale.getDefault());
            throw new BadRequestException(errorMessage);
        }

        Long competencyId = requestDto.getCompetencyId();

        CompetencyDto competencyDto = new CompetencyDto();
        competencyDto.setId(competencyId);
        competencyDto.setName(requestDto.getCompetencyName());
        competencyDto.setDescription(requestDto.getCompetencyDescription());
        competencyDto.setDeleted(false);
        competencyDto.setUpdatedBy(userId);
        competencyDto.setUpdatedAt(OffsetDateTime.now());

        CompetencyDto updatedCompetencyDto = competencyService.update(competencyId, competencyDto);

        List<CompetencyCompTagDto> existingCompetencyCompTagDtoList = competencyCompTagService.findAllByCompetencyId(competencyId);
        Set<Long> existingCompTagIds = existingCompetencyCompTagDtoList.stream()
                .map(rjs -> rjs.getId().getCompTagId())
                .collect(Collectors.toSet());

        List<CompTagDto> requestedCompTags;

        if (requestDto.getCompTagList() != null || !requestDto.getCompTagList().isEmpty()) {
            requestedCompTags = compTagService.createAll(
                    requestDto.getCompTagList().stream()
                            .map(name -> {
                                CompTagDto dto = new CompTagDto();
                                dto.setTag(name.trim());
                                dto.setDeleted(false);
                                dto.setCreatedBy(userId);
                                dto.setCreatedAt(OffsetDateTime.now());
                                dto.setUpdatedBy(userId);
                                dto.setUpdatedAt(OffsetDateTime.now());
                                return dto;
                            })
                            .collect(Collectors.toList())
            );
        } else {
            requestedCompTags = Collections.emptyList();
        }

        Set<Long> requestedCompTagIds = requestedCompTags.stream()
                .map(CompTagDto::getId)
                .collect(Collectors.toSet());

        Set<Long> toAdd = new HashSet<>(requestedCompTagIds);
        toAdd.removeAll(existingCompTagIds);

        Set<Long> toRemove = new HashSet<>(existingCompTagIds);
        toRemove.removeAll(requestedCompTagIds);

        if (!toAdd.isEmpty()) {
            List<CompetencyCompTagDto> competencyCompTagDtoList = toAdd.stream()
                    .map(compTagId -> {
                        CompetencyCompTagDto cct = new CompetencyCompTagDto();
                        cct.setId(new CompetencyCompTagId(competencyId, compTagId));
                        cct.setCompetency(updatedCompetencyDto);
                        cct.setCompTag(requestedCompTags.stream()
                                .filter(ct -> ct.getId().equals(compTagId))
                                .findFirst().orElseThrow());
                        cct.setCreatedBy(userId);
                        cct.setCreatedAt(OffsetDateTime.now());
                        cct.setUpdatedBy(userId);
                        cct.setUpdatedAt(OffsetDateTime.now());
                        return cct;
                    })
                    .collect(Collectors.toList());

            competencyCompTagService.createAll(competencyCompTagDtoList);
        }

        if (!toRemove.isEmpty()) {
            competencyCompTagService.deleteByCompetencyIdAndCompTagIdIn(competencyId, toRemove);

            Set<Long> stillUsed = competencyCompTagService.findAllUsedByOther(toRemove, competencyId).stream()
                    .map(cct -> cct.getId().getCompTagId())
                    .collect(Collectors.toSet());

            Set<Long> toDeleteCompTag = new HashSet<>(toRemove);
            toDeleteCompTag.removeAll(stillUsed);

            if (!toDeleteCompTag.isEmpty()) {
                compTagService.deleteAllByIdIn(toDeleteCompTag, userId);
            }
        }
    }

    @Override
    @Transactional
    public void delete(Long competencyId, UUID userId) {
        if (competencyId == null) {
            String errorMessage = messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{COMPETENCY_DELETE_OPERATION}, Locale.getDefault());
            throw new BadRequestException(errorMessage);
        }

        List<CompetencyCompTagDto> existingAssignedCompetencyCompTagDtoList = competencyCompTagService.findAllByCompetencyId(competencyId);

        if (!existingAssignedCompetencyCompTagDtoList.isEmpty()) {
            Set<CompetencyCompTagId> existingAssignedRoleJobScopeIds = existingAssignedCompetencyCompTagDtoList.stream()
                    .map(CompetencyCompTagDto::getId)
                    .collect(Collectors.toSet());

            Set<Long> compTagsToCheck = existingAssignedCompetencyCompTagDtoList.stream()
                    .map(rjs -> rjs.getId().getCompTagId())
                    .collect(Collectors.toSet());

            Set<Long> stillUsedCompTagIds = competencyCompTagService.findAllUsedByOther(compTagsToCheck, competencyId)
                    .stream().map(stillUsedCompTag -> stillUsedCompTag.getId().getCompTagId())
                    .collect(Collectors.toSet());

            Set<Long> compTagToRemove = new HashSet<>(compTagsToCheck);
            compTagToRemove.removeAll(stillUsedCompTagIds);

            competencyCompTagService.deleteAllByIdIn(existingAssignedRoleJobScopeIds);
            compTagService.deleteAllByIdIn(compTagToRemove, userId);
        }

        competencyService.delete(competencyId, userId);
    }

    @Override
    @Transactional
    public void bulkDelete(List<Long> competencyIds, UUID userId) {
        if (competencyIds == null || competencyIds.isEmpty()) {
            String errorMessage = messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{COMPETENCY_DELETE_OPERATION}, Locale.getDefault());
            throw new BadRequestException(errorMessage);
        }

        Set<Long> competencyIdsToRemove = new HashSet<>(competencyIds);

        List<CompetencyCompTagDto> selectedCompetencyCompTagDtoList = competencyCompTagService.findAllByCompetencyIdIn(competencyIdsToRemove);

        if (!selectedCompetencyCompTagDtoList.isEmpty()) {
            Set<CompetencyCompTagId> selectedCompetencyCompTagIds = selectedCompetencyCompTagDtoList.stream()
                    .map(CompetencyCompTagDto::getId)
                    .collect(Collectors.toSet());

            Set<Long> compTagToCheck = selectedCompetencyCompTagDtoList.stream()
                    .map(cct -> cct.getId().getCompTagId())
                    .collect(Collectors.toSet());

            Set<Long> stillUsedCompTagIds = competencyCompTagService.findAllUsedByOthers(compTagToCheck, competencyIdsToRemove)
                    .stream().map(stillUsedCompTag -> stillUsedCompTag.getId().getCompTagId())
                    .collect(Collectors.toSet());

            Set<Long> compTagToRemove = new HashSet<>(compTagToCheck);
            compTagToRemove.removeAll(stillUsedCompTagIds);

            competencyCompTagService.deleteAllByIdIn(selectedCompetencyCompTagIds);
            compTagService.deleteAllByIdIn(compTagToRemove, userId);
        }

        competencyService.deleteAllByIdIn(competencyIdsToRemove, userId);
    }

    @Override
    public ByteArrayResource exportData() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Sheet 1");
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            int rowIndex = 0;

            Row header = sheet.createRow(rowIndex);

            Cell cellCompName = header.createCell(0);
            cellCompName.setCellValue(COMPETENCY_NAME_COLUMN);
            createCellComment(drawing, cellCompName, messageSource.getMessage(COMPETENCY_NAME_NOTES, null, Locale.getDefault()));

            Cell cellCompDesc = header.createCell(1);
            cellCompDesc.setCellValue(COMPETENCY_DESC_COLUMN);
            createCellComment(drawing, cellCompDesc, messageSource.getMessage(COMPETENCY_DESC_NOTES, null, Locale.getDefault()));

            Cell cellTags = header.createCell(2);
            cellTags.setCellValue(TAGS_COLUMN);
            createCellComment(drawing, cellTags, messageSource.getMessage(TAGS_NOTES, null, Locale.getDefault()));

            Cell cellNewName = header.createCell(3);
            cellNewName.setCellValue(NEW_COMPETENCY_NAME_COLUMN);
            createCellComment(drawing, cellNewName, messageSource.getMessage(NEW_COMPETENCY_NAME_NOTES, null, Locale.getDefault()));

            Cell cellDeleted = header.createCell(4);
            cellDeleted.setCellValue(TO_BE_DELETED_COLUMN);
            createCellComment(drawing, cellDeleted, messageSource.getMessage(TO_BE_DELETED_NOTES, null, Locale.getDefault()));

            rowIndex++;

            List<CompetencyDto> allCompetency = competencyService.findAllByIsDeletedIsFalse();
            List<CompetencyCompTagDto> allTagsAssigned = competencyCompTagService.findAll();

            for (CompetencyDto dto : allCompetency) {
                List<String> assignedTags = allTagsAssigned.stream().filter(compTag
                        -> Objects.equals(compTag.getId().getCompetencyId(), dto.getId()))
                        .map(compTag -> compTag.getCompTag().getTag())
                        .toList();

                String tags = assignedTags.isEmpty() ? "" : String.join("; ", assignedTags);

                Row row = sheet.createRow(rowIndex);
                row.createCell(0).setCellValue(dto.getName());
                row.createCell(1).setCellValue(dto.getDescription());
                row.createCell(2).setCellValue(tags);

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
            String errorMessage = messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{IMPORT_OPERATION}, Locale.getDefault());
            throw new BadRequestException(errorMessage);
        }

        if (file.getSize() > MAX_FILE_SIZE_IN_MB * 1024 * 1024) {
            String errorMessage = messageSource.getMessage(IMPORT_FILE_TOO_LARGE_ERR_MSG_CODE, null, Locale.getDefault());
            throw new BadRequestException(errorMessage);
        }

        List<String> acceptedTypes = List.of(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/octet-stream",
                ""
        );

        if (!acceptedTypes.contains(file.getContentType())) {
            String errorMessage = messageSource.getMessage(IMPORT_FILE_INVALID_ERR_MSG_CODE, null, Locale.getDefault());
            throw new BadRequestException(errorMessage);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(ACCEPTED_IMPORT_FILE_TYPE)) {
            throw new BadRequestException(messageSource.getMessage(IMPORT_FILE_INVALID_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(8));

        List<CompetencyDto> allCompetency = competencyService.findAllByIsDeletedIsFalse();

        Map<String, CompetencyDto> allCompetencyMap = allCompetency.stream().collect(Collectors.toMap(
                dto -> dto.getName().toLowerCase().trim(),
                Function.identity()
        ));

        List<CompetencyCompTagDto> allAssignedTag = competencyCompTagService.findAll();

        Sheet sheet = workbook.getSheetAt(0);

        Row header = sheet.getRow(0);
        if (!getCellValueAsString(header.getCell(0)).equalsIgnoreCase(COMPETENCY_NAME_COLUMN)
                || !getCellValueAsString(header.getCell(1)).equalsIgnoreCase(COMPETENCY_DESC_COLUMN)
                || !getCellValueAsString(header.getCell(2)).equalsIgnoreCase(TAGS_COLUMN)
                || !getCellValueAsString(header.getCell(3)).equalsIgnoreCase(NEW_COMPETENCY_NAME_COLUMN)
                || !getCellValueAsString(header.getCell(4)).equalsIgnoreCase(TO_BE_DELETED_COLUMN)) {
            throw new BadRequestException(messageSource.getMessage(IMPORT_FILE_INVALID_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        List<CompetencyDto> competencyToBeUpdatedOrCreated = new ArrayList<>();
        Set<String> tagsToBeCreated = new HashSet<>();
        Map<String, Set<String>> competencyCompTagToBeCreated = new HashMap<>();
        Set<CompetencyCompTagId> competencyCompTagIdsToBeDeleted = new HashSet<>();

        Set<String> duplicatedRow = new HashSet<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || row.getCell(0) == null || getCellValueAsString(row.getCell(0)) == null
                    || getCellValueAsString(row.getCell(0)).isBlank()) {
                continue;
            }

            String competencyName = getCellValueAsString(row.getCell(0));
            String competencyDescription = getCellValueAsString(row.getCell(1));
            String tags = getCellValueAsString(row.getCell(2));
            String newCompetencyName = getCellValueAsString(row.getCell(3));
            String toBeDeleted = getCellValueAsString(row.getCell(4));

            Set<String> inputtedTags;
            if (!validationService.isNullOrBlank(tags)) {
                int finalI = i;
                inputtedTags = Arrays.stream(tags.split(";"))
                        .map(String::trim)
                        .filter(tag -> !validationService.isNullOrBlank(tag))
                        .peek(tag -> {
                            if (tag.length() > 100) {
                                String errorMessage = messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE, new String[]{TAGS_COLUMN, Integer.toString(finalI + 1)}, Locale.getDefault());
                                throw new BadRequestException(errorMessage);
                            }
                        })
                        .collect(Collectors.toSet());
            } else {
                inputtedTags = Collections.emptySet();
            }

            if (validationService.isNullOrBlank(competencyName) || competencyName.length() > 255) {
                String errorMessage = messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE,
                        new String[]{COMPETENCY_NAME_COLUMN, Integer.toString(i + 1)}, Locale.getDefault());
                throw new BadRequestException(errorMessage);
            }

            if (!validationService.isNullOrBlank(competencyDescription) && competencyDescription.trim().length() > 1000) {
                String errorMessage = messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE,
                        new String[]{COMPETENCY_DESC_COLUMN, Integer.toString(i + 1)}, Locale.getDefault());
                throw new BadRequestException(errorMessage);
            }

            if (toBeDeleted != null && toBeDeleted.equalsIgnoreCase(YES)
                    && !validationService.isNullOrBlank(newCompetencyName)) {
                String errorMessage = messageSource.getMessage(IMPORT_ACTION_CONFLICT_ERR_MSG_CODE,
                        new String[]{Integer.toString(i + 1)}, Locale.getDefault());
                throw new BadRequestException(errorMessage);
            }

            if (duplicatedRow.contains(competencyName.toLowerCase().trim())) {
                String errorMessage = messageSource.getMessage(IMPORT_DUPLICATE_ENTRY_ERR_MSG_CODE, new String[]{Integer.toString(i + 1)}, Locale.getDefault());
                throw new BadRequestException(errorMessage);
            } else {
                duplicatedRow.add(competencyName.toLowerCase().trim());
            }

            CompetencyDto competencyDto;
            Map<String, CompetencyCompTagDto> assignedCompTagMap = new HashMap<>();
            Set<String> assignedCompTags = new HashSet<>();

            if (allCompetencyMap.containsKey(competencyName.toLowerCase().trim())) {
                competencyDto = Objects.requireNonNull(allCompetencyMap.get(competencyName.toLowerCase().trim()));
                competencyDto.setUpdatedBy(userId);
                competencyDto.setUpdatedAt(now);

                if (!validationService.isNullOrBlank(newCompetencyName)) {
                    if (allCompetencyMap.containsKey(newCompetencyName.toLowerCase().trim())) {
                        String errorMessage = messageSource.getMessage(IMPORT_UNIQUE_NAME_ERR_MSG_CODE,
                                new String[]{newCompetencyName, Integer.toString(i + 1)}, Locale.getDefault());
                        throw new BadRequestException(errorMessage);
                    }
                    competencyDto.setName(newCompetencyName.trim());
                    allCompetencyMap.remove(competencyName.toLowerCase().trim());
                    allCompetencyMap.put(newCompetencyName.toLowerCase().trim(), competencyDto);
                }

                assignedCompTagMap.putAll(allAssignedTag.stream().filter(dto
                        -> Objects.equals(dto.getId().getCompetencyId(), competencyDto.getId()))
                        .collect(Collectors.toMap(
                                dto -> dto.getCompTag().getTag().toLowerCase().trim(),
                                Function.identity()
                        )));
                assignedCompTags.addAll(assignedCompTagMap.keySet());

                if (!validationService.isNullOrBlank(toBeDeleted) && toBeDeleted.equalsIgnoreCase(YES)) {
                    competencyDto.setDeleted(true);
                    competencyToBeUpdatedOrCreated.add(competencyDto);
                    competencyCompTagIdsToBeDeleted.addAll(assignedCompTagMap.values().stream()
                            .map(CompetencyCompTagDto::getId).collect(Collectors.toSet()));
                    continue;
                }

                competencyDto.setDescription(validationService.isNullOrBlank(competencyDescription) ? null : competencyDescription.trim());
            } else {
                if (!validationService.isNullOrBlank(newCompetencyName)
                        || (!validationService.isNullOrBlank(toBeDeleted) && toBeDeleted.equalsIgnoreCase(YES))) {
                    String errorMessage = messageSource.getMessage(IMPORT_ACTION_CONFLICT_ERR_MSG_CODE,
                            new String[]{Integer.toString(i + 1)}, Locale.getDefault());
                    throw new BadRequestException(errorMessage);
                }

                if (allCompetencyMap.containsKey(competencyName.trim().toLowerCase())) {
                    String errorMessage = messageSource.getMessage(IMPORT_UNIQUE_NAME_ERR_MSG_CODE,
                            new String[]{competencyName, Integer.toString(i + 1)}, Locale.getDefault());
                    throw new BadRequestException(errorMessage);
                }

                competencyDto = new CompetencyDto(
                        competencyName.trim(),
                        validationService.isNullOrBlank(competencyDescription) ? null : competencyDescription.trim(),
                        false,
                        userId,
                        now,
                        userId,
                        now
                );

                allCompetencyMap.put(competencyName.toLowerCase().trim(), competencyDto);
            }

            Set<String> lowerCaseInputtedTags = inputtedTags.stream()
                    .map(tag -> tag.toLowerCase().trim())
                    .collect(Collectors.toSet());
            Set<String> toAdd = new HashSet<>(lowerCaseInputtedTags);
            toAdd.removeAll(assignedCompTags);

            Set<String> toRemove = new HashSet<>(assignedCompTags);
            toRemove.removeAll(lowerCaseInputtedTags);

            if (!toRemove.isEmpty()) {
                Set<CompetencyCompTagId> idToRemove = toRemove.stream().map(tagToRemove
                        -> Objects.requireNonNull(assignedCompTagMap.get(tagToRemove.toLowerCase().trim()).getId()))
                        .collect(Collectors.toSet());
                competencyCompTagIdsToBeDeleted.addAll(idToRemove);
            }

            if (!toAdd.isEmpty()) {
                Set<String> toAddBeforeToLowerCase = inputtedTags.stream().filter(tag
                        -> toAdd.contains(tag.toLowerCase().trim()))
                        .collect(Collectors.toSet());
                tagsToBeCreated.addAll(toAddBeforeToLowerCase);
                competencyCompTagToBeCreated.put(competencyDto.getName().toLowerCase().trim(), toAddBeforeToLowerCase);
            }

            competencyToBeUpdatedOrCreated.add(competencyDto);
        }

        Map<String, CompetencyDto> createdCompetencyMap;
        if (!competencyToBeUpdatedOrCreated.isEmpty()) {
            createdCompetencyMap = competencyService.createAndUpdateAll(competencyToBeUpdatedOrCreated)
                    .stream().collect(Collectors.toMap(
                            dto -> dto.getName().toLowerCase().trim(),
                            Function.identity()
                    ));
        } else {
            createdCompetencyMap = Collections.emptyMap();
        }

        if (!tagsToBeCreated.isEmpty()) {
            List<CompTagDto> compTagDtoToBeCreated = tagsToBeCreated.stream().map(tag
                    -> new CompTagDto(tag.trim(), false, userId, now, userId, now))
                    .toList();

            Map<String, CompTagDto> createdTagMap = compTagService.createAll(compTagDtoToBeCreated).stream()
                    .collect(Collectors.toMap(CompTagDto::getTag, Function.identity()));

            List<CompetencyCompTagDto> competencyCompTagDtoToBeCreated = competencyCompTagToBeCreated.entrySet().stream().flatMap(entrySet -> {
                CompetencyDto competencyDto = Objects.requireNonNull(createdCompetencyMap.get(entrySet.getKey()));
                List<CompTagDto> compTagDtoList = entrySet.getValue().stream().map(createdTagMap::get).toList();
                return compTagDtoList.stream().map(compTagDto
                        -> new CompetencyCompTagDto(
                                new CompetencyCompTagId(competencyDto.getId(), compTagDto.getId()),
                                competencyDto,
                                compTagDto,
                                userId,
                                now,
                                userId,
                                now
                        ));
            }).toList();

            competencyCompTagService.createAll(competencyCompTagDtoToBeCreated);
        }

        if (!competencyCompTagIdsToBeDeleted.isEmpty()) {
            competencyCompTagService.deleteAllByIdIn(competencyCompTagIdsToBeDeleted);

            Set<Long> tagIdToCheck = competencyCompTagIdsToBeDeleted.stream().map(CompetencyCompTagId::getCompTagId).collect(Collectors.toSet());
            Set<Long> stillInUsed = competencyCompTagService.findAllByCompTagIdIn(tagIdToCheck)
                    .stream().map(dto -> dto.getId().getCompTagId()).collect(Collectors.toSet());

            Set<Long> toRemove = new HashSet<>(tagIdToCheck);
            toRemove.removeAll(stillInUsed);

            competencyService.deleteAllByIdIn(toRemove, userId);
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
        }
        if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return cell.getStringCellValue().trim();
    }
}
