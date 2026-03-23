package com.tbm.careerpathlearning.service;

import com.tbm.careerpathlearning.dto.OrgChartGraphDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface OrgChartManagementService {
    List<OrgChartGraphDto> getOrgChartGraph();
    byte[] exportData() throws IOException;
    void importData(MultipartFile file, UUID userId) throws IOException;
}
