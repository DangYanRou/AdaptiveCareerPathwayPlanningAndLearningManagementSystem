package com.tbm.careerpathlearning.service;

import com.tbm.careerpathlearning.dto.CareerPathwayOverviewDto;
import com.tbm.careerpathlearning.dto.CreateCareerPathwayRequestDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CareerPathwayManagementService {
    List<CareerPathwayOverviewDto> getOverview();
    void create(CreateCareerPathwayRequestDto req, UUID userId);
    void update(CreateCareerPathwayRequestDto req, UUID userId);
    void delete(Long id, UUID userId);
    void bulkDelete(Set<Long> ids, UUID userId);
    byte[] exportData() throws IOException;
    void importData(MultipartFile file, UUID userId) throws IOException;
    CareerPathwayOverviewDto getMyCareerPathway(UUID staffId);
}
