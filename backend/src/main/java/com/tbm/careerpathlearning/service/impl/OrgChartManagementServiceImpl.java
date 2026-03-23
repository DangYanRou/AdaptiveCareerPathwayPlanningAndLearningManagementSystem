package com.tbm.careerpathlearning.service.impl;

import com.tbm.careerpathlearning.dto.*;
import com.tbm.careerpathlearning.enums.AuthorityName;
import com.tbm.careerpathlearning.enums.OrgChartType;
import com.tbm.careerpathlearning.enums.RelationType;
import com.tbm.careerpathlearning.exception.BadRequestException;
import com.tbm.careerpathlearning.model.RoleAuthorityId;
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
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrgChartManagementServiceImpl implements OrgChartManagementService {

    @Autowired
    private OrgChartService orgChartService;

    @Autowired
    private ParentChildNodeService parentChildNodeService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private StaffProfileService staffProfileService;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private RoleAuthorityService roleAuthorityService;

    @Autowired
    private RoleCompetencyService roleCompetencyService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private MessageSource messageSource;

    private static final String TYPE_NOTES = "orgchart.type.notes";
    private static final String NAME_NOTES = "orgchart.name.notes";
    private static final String PARENT_NAME_NOTES = "orgchart.parent.name.notes";
    private static final String RENAME_NOTES = "orgchart.rename.notes";
    private static final String DELETED_NOTES = "orgchart.deleted.notes";
    private static final String INVALID_REQUEST_BODY_ERR_MSG_CODE = "invalid.request.body.err.msg";
    private static final String IMPORT_OPERATION = "Import Organizational Chart Data";
    private static final String IMPORT_INVALID_DATA_ERR_MSG_CODE = "import.invalid.data.err.msg";
    private static final String NAME_COLUMN = "Node Name";
    private static final String TYPE_COLUMN = "Node Type";
    private static final String IMPORT_UNIQUE_NAME_ERR_MSG_CODE = "import.unique.name.err.msg";
    private static final String IMPORT_PARENT_NAME_CONFLICT_ERR_MSG_CODE = "import.parent.name.conflict.err.msg";
    private static final String IMPORT_RECURSIVE_REFERENCE_ERR_MSG_CODE = "import.recursive.reference.err.msg";
    private static final String IMPORT_ORG_CHART_TYPE_CHANGE_ERR_MSG_CODE = "import.org.chart.type.change.err.msg";
    private static final String IMPORT_ACTION_CONFLICT_ERR_MSG_CODE = "import.action.conflict.err.msg";
    private static final String IMPORT_INVALID_HIERARCHY_ERR_MSG_CODE = "import.invalid.hierarchy.err.msg";
    private static final String IMPORT_FILE_TOO_LARGE_ERR_MSG_CODE = "import.file.too.large.err.msg";
    private static final String IMPORT_FILE_INVALID_ERR_MSG_CODE = "import.file.invalid.err.msg";
    private static final String IMPORT_CHILD_NODE_NULL_ERR_MSG_CODE = "import.org.chart.child.null.err.msg";
    private static final String IMPORT_PARENT_NODE_NULL_ERR_MSG_CODE = "import.org.chart.parent.null.err.msg";
    private static final String YES = "Yes";
    private static final int MAX_FILE_SIZE_IN_MB = 5;
    private static final String ACCEPTED_IMPORT_FILE_TYPE = ".xlsx";

    @Override
    public List<OrgChartGraphDto> getOrgChartGraph() {
        List<OrgChartDto> orgChartDtoList = orgChartService.findAllByIsDeletedIsFalse();
        List<ParentChildNodeDto> relations = parentChildNodeService.findAllByRelationType(RelationType.ORG_CHART);
        List<RoleDto> roleDtoList = roleService.getAllByDeletedIsFalse();
        List<StaffDto> staffDtoList = staffService.findAllByIsDeletedIsFalse();

        Map<Long, List<Long>> childrenMap = new HashMap<>(relations.size());
        relations.forEach(rel ->
                childrenMap.computeIfAbsent(rel.getParentId(), k -> new ArrayList<>()).add(rel.getChildId())
        );

        Map<Long, OrgChartDepartmentNodeDto> departmentNodeMap = orgChartDtoList.stream()
                .filter(dto -> Objects.equals(dto.getType(), OrgChartType.D))
                .collect(Collectors.toMap(
                        OrgChartDto::getId,
                        dto -> {
                            Map<Long, RoleDto> assignedRoleMap = roleDtoList.stream()
                                    .filter(role -> Objects.equals(role.getOrgChart().getId(), dto.getId()))
                                    .collect(Collectors.toMap(RoleDto::getId, Function.identity()));

                            Map<Long, List<StaffDto>> assignedStaffMap = assignedRoleMap.keySet().stream().collect(Collectors.toMap(
                                    Function.identity(),
                                    key -> staffDtoList.stream()
                                            .filter(staff -> staff.getRole() != null && Objects.equals(staff.getRole().getId(), key))
                                            .collect(Collectors.toList())
                            ));

                            Map<Long, Integer> roleSatffMap = assignedRoleMap.keySet().stream().collect(Collectors.toMap(
                                    Function.identity(),
                                    key -> assignedStaffMap.get(key).size()
                            ));

                            return new OrgChartDepartmentNodeDto(
                                    dto.getId(),
                                    dto.getName(),
                                    assignedRoleMap,
                                    assignedStaffMap,
                                    roleSatffMap
                            );
                        }));

        Map<Long, OrgChartPersonNodeDto> personNodeMap = orgChartDtoList.stream()
                .filter(dto -> Objects.equals(dto.getType(), OrgChartType.P))
                .collect(Collectors.toMap(
                        OrgChartDto::getId,
                        dto -> {
                            RoleDto roleDto = Objects.requireNonNull(
                                    roleDtoList.stream().filter(role -> Objects.equals(role.getOrgChart().getId(), dto.getId()))
                                            .findAny().orElse(null)
                            );

                            StaffDto staffDto = staffDtoList.stream()
                                    .filter(staff -> staff.getRole() != null && Objects.equals(staff.getRole().getId(), roleDto.getId()))
                                    .findAny().orElse(null);

                            StaffProfileDto staffProfileDto = staffDto != null
                                    ? staffProfileService.findById(staffDto.getId())
                                    : null;

                            return new OrgChartPersonNodeDto(
                                    staffDto == null ? null : staffDto.getId(),
                                    dto.getId(),
                                    roleDto.getId(),
                                    roleDto.getName(),
                                    staffDto == null ? null : staffDto.getName(),
                                    staffDto == null ? null : staffDto.getEmail(),
                                    staffProfileDto == null ? null : staffProfileDto.getProfilePicturePath()
                            );
                        }));

        Map<Long, OrgChartGraphDto> graphMap = orgChartDtoList.stream().collect(Collectors.toMap(
                OrgChartDto::getId,
                dto -> {
                    OrgChartGraphNodeDto node = new OrgChartGraphNodeDto(
                            dto.getType().getKey(),
                            dto.getType() == OrgChartType.D ? departmentNodeMap.get(dto.getId()) : null,
                            dto.getType() == OrgChartType.P ? personNodeMap.get(dto.getId()) : null
                    );
                    return new OrgChartGraphDto(dto.getId(), dto.getName(), node, new ArrayList<>());
                }
        ));

        for (Map.Entry<Long, List<Long>> entry : childrenMap.entrySet()) {
            OrgChartGraphDto parentNode = graphMap.get(entry.getKey());
            if (parentNode != null) {
                List<OrgChartGraphDto> childNodes = entry.getValue().stream()
                        .map(graphMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                parentNode.setChildren(childNodes);
            }
        }

        Set<Long> allParents = childrenMap.keySet();
        Set<Long> allChildren = childrenMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Set<Long> rootIds = allParents.stream()
                .filter(p -> !allChildren.contains(p))
                .collect(Collectors.toSet());

        return rootIds.stream()
                .map(graphMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] exportData() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Sheet 1");
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            int rowIndex = 0;

            Row header = sheet.createRow(rowIndex);

            Cell cellType = header.createCell(0);
            cellType.setCellValue("Node Type");
            createCellComment(drawing, cellType, messageSource.getMessage(TYPE_NOTES, null, Locale.getDefault()));

            Cell cellName = header.createCell(1);
            cellName.setCellValue("Node Name");
            createCellComment(drawing, cellName, messageSource.getMessage(NAME_NOTES, null, Locale.getDefault()));

            Cell cellParentName = header.createCell(2);
            cellParentName.setCellValue("Parent Node Name");
            createCellComment(drawing, cellParentName, messageSource.getMessage(PARENT_NAME_NOTES, null, Locale.getDefault()));

            Cell cellRename = header.createCell(3);
            cellRename.setCellValue("Rename To");
            createCellComment(drawing, cellRename, messageSource.getMessage(RENAME_NOTES, null, Locale.getDefault()));

            Cell cellDeleted = header.createCell(4);
            cellDeleted.setCellValue("To Be Deleted");
            createCellComment(drawing, cellDeleted, messageSource.getMessage(DELETED_NOTES, null, Locale.getDefault()));

            rowIndex++;

            Map<Long, OrgChartDto> existedOrgChartMap = orgChartService.findAllByIsDeletedIsFalse().stream()
                    .collect(Collectors.toMap(OrgChartDto::getId, Function.identity()));

            List<ParentChildNodeDto> existedParentChildNodeDtos = parentChildNodeService.findAllByRelationType(RelationType.ORG_CHART);

            for (OrgChartDto orgChartDto : existedOrgChartMap.values()) {
                List<ParentChildNodeDto> parentNodes = existedParentChildNodeDtos.stream()
                        .filter(dto -> Objects.equals(dto.getChildId(), orgChartDto.getId()))
                        .toList();

                if (!parentNodes.isEmpty()) {
                    for (ParentChildNodeDto parentNode : parentNodes) {
                        Row row = sheet.createRow(rowIndex);
                        row.createCell(0).setCellValue(orgChartDto.getType().getKey());
                        row.createCell(1).setCellValue(orgChartDto.getName());
                        row.createCell(2).setCellValue(existedOrgChartMap.get(parentNode.getParentId()).getName());
                        rowIndex++;
                    }
                } else {
                    Row row = sheet.createRow(rowIndex);
                    row.createCell(0).setCellValue(orgChartDto.getType().getKey());
                    row.createCell(1).setCellValue(orgChartDto.getName());
                    rowIndex++;
                }
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

        List<OrgChartDto> existingOrgChartDtos = orgChartService.findAllByIsDeletedIsFalse();
        List<String> existingOrgChartName = existingOrgChartDtos.stream().map(dto -> dto.getName().toLowerCase().trim()).toList();
        Set<String> nameSeen = new HashSet<>(existingOrgChartName);
        Map<Integer, String> rootNameMap = new HashMap<>();
        Map<Integer, String> nonRootNameMap = new HashMap<>();

        List<String> roleNameToBeAdded = new ArrayList<>();
        Set<OrgChartDto> orgChartDtoToBeAddedOrUpdated = new HashSet<>();
        Set<Long> nodeIdToBeDeleted = new HashSet<>();
        Set<Long> roleToBeRemoved = new HashSet<>();
        Map<String, List<String>> hierarchyGraph = new HashMap<>();

        Sheet sheet = workbook.getSheetAt(0);

        Row header = sheet.getRow(0);
        if (!getCellValueAsString(header.getCell(0)).equalsIgnoreCase("Node Type")
                || !getCellValueAsString(header.getCell(1)).equalsIgnoreCase("Node Name")
                || !getCellValueAsString(header.getCell(2)).equalsIgnoreCase("Parent Node Name")
                || !getCellValueAsString(header.getCell(3)).equalsIgnoreCase("Rename To")
                || !getCellValueAsString(header.getCell(4)).equalsIgnoreCase("To Be Deleted")) {
            throw new BadRequestException(messageSource.getMessage(IMPORT_FILE_INVALID_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || row.getCell(0) == null || getCellValueAsString(row.getCell(0)) == null
                    || getCellValueAsString(row.getCell(0)).isBlank()) continue;

            String type = getCellValueAsString(row.getCell(0));
            String name = getCellValueAsString(row.getCell(1));
            String parentName = getCellValueAsString(row.getCell(2));
            String renameTo = getCellValueAsString(row.getCell(3));
            String toBeDeleted = getCellValueAsString(row.getCell(4));

            if (validationService.isNullOrBlank(name) || name.length() > 255
                    || (!validationService.isNullOrBlank(renameTo) && renameTo.trim().length() > 255)) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE, new String[]{NAME_COLUMN, Integer.toString(i + 1)}, Locale.getDefault()));
            }

            if (!type.equalsIgnoreCase("P") && !type.equalsIgnoreCase("D")) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_INVALID_DATA_ERR_MSG_CODE, new String[]{TYPE_COLUMN, Integer.toString(i + 1)}, Locale.getDefault()));
            }

            if (toBeDeleted != null && toBeDeleted.equalsIgnoreCase(YES) && !validationService.isNullOrBlank(renameTo)) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_ACTION_CONFLICT_ERR_MSG_CODE, new String[]{Integer.toString(i + 1)}, Locale.getDefault()));
            }

            if (validationService.isNullOrBlank(parentName)) {
                if (nonRootNameMap.containsValue(name.toLowerCase())) {
                    throw new BadRequestException(messageSource.getMessage(IMPORT_PARENT_NAME_CONFLICT_ERR_MSG_CODE,
                            new String[]{String.valueOf(getKeyFromValue(nonRootNameMap, name.toLowerCase())) + 1, Integer.toString(i + 1)},
                            Locale.getDefault()));
                }
                rootNameMap.put(i, name.toLowerCase());
            } else if (validationService.isNullOrBlank(toBeDeleted)) {
                if (rootNameMap.containsValue(name.toLowerCase())) {
                    throw new BadRequestException(messageSource.getMessage(IMPORT_PARENT_NAME_CONFLICT_ERR_MSG_CODE,
                            new String[]{Integer.toString(i + 1), String.valueOf(getKeyFromValue(rootNameMap, name.toLowerCase())) + 1},
                            Locale.getDefault()));
                }

                if (Objects.equals(parentName.toLowerCase(), name.toLowerCase())) {
                    throw new BadRequestException(messageSource.getMessage(IMPORT_RECURSIVE_REFERENCE_ERR_MSG_CODE,
                            new String[]{Integer.toString(i + 1)},
                            Locale.getDefault()));
                }

                nonRootNameMap.put(i, name.toLowerCase());
            }

            if (existingOrgChartName.contains(name.toLowerCase())) {
                OrgChartDto orgChartDto = Objects.requireNonNull(existingOrgChartDtos.stream()
                        .filter(dto -> name.equalsIgnoreCase(dto.getName()))
                        .findFirst().orElse(null));

                boolean isUpdating = false;

                if (toBeDeleted != null && toBeDeleted.equalsIgnoreCase(YES)) {
                    nodeIdToBeDeleted.add(orgChartDto.getId());
                    if (type.equalsIgnoreCase("P")) {
                        roleToBeRemoved.add(orgChartDto.getId());
                    }
                    continue;
                }

                if (!type.equalsIgnoreCase(orgChartDto.getType().getKey())) {
                    throw new BadRequestException(messageSource.getMessage(IMPORT_ORG_CHART_TYPE_CHANGE_ERR_MSG_CODE,
                            new String[]{type, Integer.toString(i + 1)},
                            Locale.getDefault()));
                }

                if (validationService.isNullOrBlank(parentName) && !orgChartDto.isRoot()) {
                    orgChartDto.setRoot(true);
                    isUpdating = true;
                }

                if (!validationService.isNullOrBlank(renameTo)) {
                    String renameDuplicated = !nameSeen.add(renameTo.toLowerCase()) ? renameTo : null;
                    if (renameDuplicated != null) {
                        throw new BadRequestException(messageSource.getMessage(IMPORT_UNIQUE_NAME_ERR_MSG_CODE,
                                new String[]{name, Integer.toString(i + 1)},
                                Locale.getDefault()));
                    }
                    orgChartDto.setName(renameTo);
                    isUpdating = true;

                    if (!orgChartDto.isRoot()) {
                        hierarchyGraph.computeIfAbsent(parentName.toLowerCase(), k -> new ArrayList<>())
                                .add(renameTo.toLowerCase());
                    }

                    if (hierarchyGraph.get(name.toLowerCase()) != null) {
                        List<String> existingChildren = new ArrayList<>(hierarchyGraph.get(name.toLowerCase()));
                        if (!existingChildren.isEmpty()) {
                            if (hierarchyGraph.get(renameTo.toLowerCase()) != null) {
                                List<String> upcomingChildren = new ArrayList<>(hierarchyGraph.get(renameTo.toLowerCase()));
                                if (!upcomingChildren.isEmpty()) {
                                    existingChildren.addAll(upcomingChildren);
                                }
                            }
                            hierarchyGraph.computeIfAbsent(renameTo.toLowerCase(), k -> existingChildren);
                        }
                    }

                    hierarchyGraph.remove(name.toLowerCase());
                } else {
                    if (!orgChartDto.isRoot()) {
                        hierarchyGraph.computeIfAbsent(parentName.toLowerCase(), k -> new ArrayList<>())
                                .add(name.toLowerCase());
                    }
                }

                if (isUpdating) {
                    orgChartDto.setUpdatedBy(userId);
                    orgChartDto.setUpdatedAt(now);
                }

                orgChartDtoToBeAddedOrUpdated.add(orgChartDto);

            } else {
                if ((toBeDeleted != null && toBeDeleted.equalsIgnoreCase(YES)) || !validationService.isNullOrBlank(renameTo)) {
                    throw new BadRequestException(messageSource.getMessage(IMPORT_ACTION_CONFLICT_ERR_MSG_CODE,
                            new String[]{Integer.toString(i + 1)},
                            Locale.getDefault()));
                }

                if (validationService.isNullOrBlank(parentName)) {
                    orgChartDtoToBeAddedOrUpdated.add(new OrgChartDto(
                            Objects.requireNonNull(OrgChartType.fromKey(type.toUpperCase()).orElse(null)),
                            name, true, false, userId, now, userId, now
                    ));
                } else {
                    orgChartDtoToBeAddedOrUpdated.add(new OrgChartDto(
                            Objects.requireNonNull(OrgChartType.fromKey(type.toUpperCase()).orElse(null)),
                            name, false, false, userId, now, userId, now
                    ));
                    hierarchyGraph.computeIfAbsent(parentName.toLowerCase(), k -> new ArrayList<>())
                            .add(name.toLowerCase());
                }

                if (type.equalsIgnoreCase(OrgChartType.P.getKey())) {
                    roleNameToBeAdded.add(name);
                }
            }
        }

        validateNoCycles(hierarchyGraph);

        parentChildNodeService.deleteAllByParentIdInOrChildIdIn(nodeIdToBeDeleted);
        orgChartService.deleteAllByIdIn(nodeIdToBeDeleted, userId, now);

        Map<String, OrgChartDto> orgChartDtoUpdatedAndCreatedMap;
        if (!orgChartDtoToBeAddedOrUpdated.isEmpty()) {
            orgChartDtoUpdatedAndCreatedMap = orgChartService
                    .createAndUpdateAll(orgChartDtoToBeAddedOrUpdated.stream().toList())
                    .stream().collect(Collectors.toMap(
                            dto -> dto.getName().toLowerCase(),
                            Function.identity()
                    ));
        } else {
            orgChartDtoUpdatedAndCreatedMap = Collections.emptyMap();
        }

        if (!roleNameToBeAdded.isEmpty()) {
            List<RoleDto> roleDtoToBeAdded = roleNameToBeAdded.stream().map(name -> new RoleDto(
                            name.trim(), null, true, false, userId, now, userId, now,
                            orgChartDtoUpdatedAndCreatedMap.get(name.toLowerCase())))
                    .toList();

            List<RoleDto> createdRole = roleService.createAll(roleDtoToBeAdded);

            AuthorityDto authorityDto = authorityService.findByName(AuthorityName.ROLE_USER);

            List<RoleAuthorityDto> roleAuthorityToBeCreated = createdRole.stream().map(role ->
                            new RoleAuthorityDto(
                                    new RoleAuthorityId(role.getId(), authorityDto.getId()),
                                    role, authorityDto, userId, now, userId, now
                            ))
                    .toList();

            roleAuthorityService.createAll(roleAuthorityToBeCreated);
        }

        if (!roleToBeRemoved.isEmpty()) {
            Set<Long> deletedRoleIds = roleService.findAndDeleteAllByOrgChartIdIn(roleToBeRemoved, userId, now)
                    .stream().map(RoleDto::getId).collect(Collectors.toSet());
            roleAuthorityService.deleteAllByRoleIdIn(deletedRoleIds);
            roleCompetencyService.deleteAllByRoleIdIn(deletedRoleIds);
        }

        if (!hierarchyGraph.isEmpty()) {
            List<ParentChildNodeDto> linkageToBeUpdatedOrCreated = hierarchyGraph.entrySet().stream().flatMap(map -> {
                if (orgChartDtoUpdatedAndCreatedMap.containsKey(map.getKey())) {
                    Long parentId = orgChartDtoUpdatedAndCreatedMap.get(map.getKey()).getId();

                    if (orgChartDtoUpdatedAndCreatedMap.keySet().containsAll(map.getValue())) {
                        Set<Long> childIds = map.getValue().stream()
                                .map(id -> orgChartDtoUpdatedAndCreatedMap.get(id).getId())
                                .collect(Collectors.toSet());

                        return childIds.stream().map(childId -> new ParentChildNodeDto(
                                RelationType.ORG_CHART, parentId, childId, userId, now, userId, now));
                    } else {
                        throw new BadRequestException(messageSource.getMessage(IMPORT_CHILD_NODE_NULL_ERR_MSG_CODE,
                                new String[]{map.getValue().toString()}, Locale.getDefault()));
                    }
                } else {
                    throw new BadRequestException(messageSource.getMessage(IMPORT_PARENT_NODE_NULL_ERR_MSG_CODE,
                            new String[]{map.getKey()}, Locale.getDefault()));
                }
            }).toList();

            parentChildNodeService.createALl(linkageToBeUpdatedOrCreated, RelationType.ORG_CHART);
        }
    }

    private void validateNoCycles(Map<String, List<String>> graph) {
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();
        for (String node : graph.keySet()) {
            if (detectCycle(node, graph, visited, recStack)) {
                throw new BadRequestException(messageSource.getMessage(IMPORT_INVALID_HIERARCHY_ERR_MSG_CODE, new String[]{node}, Locale.getDefault()));
            }
        }
    }

    private boolean detectCycle(String node, Map<String, List<String>> graph,
                                 Set<String> visited, Set<String> recStack) {
        if (recStack.contains(node)) return true;
        if (visited.contains(node)) return false;
        visited.add(node);
        recStack.add(node);
        for (String child : graph.getOrDefault(node, new ArrayList<>())) {
            if (detectCycle(child, graph, visited, recStack)) return true;
        }
        recStack.remove(node);
        return false;
    }

    private static <K, V> K getKeyFromValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (Objects.equals(entry.getValue(), value)) return entry.getKey();
        }
        return null;
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
}
