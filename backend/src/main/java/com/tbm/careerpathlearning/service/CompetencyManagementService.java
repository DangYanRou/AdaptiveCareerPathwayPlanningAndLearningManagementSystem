package com.tbm.careerpathlearning.service;

import com.tbm.careerpathlearning.dto.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

public interface CompetencyManagementService {

    List<CompetencyOverviewDto> getOverview();

    void create(CreateCompetencyRequestDto requestDto, UUID userId);

    void update(EditCompetencyRequestDto requestDto, UUID userId);

    void delete(Long competencyId, UUID userId);

    void bulkDelete(List<Long> competencyIds, UUID userId);

    ByteArrayResource exportData() throws IOException;

    void importData(MultipartFile file, UUID userId) throws IOException;
}
