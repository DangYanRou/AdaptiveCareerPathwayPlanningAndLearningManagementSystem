package com.tbm.careerpathlearning.service;

import com.tbm.careerpathlearning.dto.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

public interface CareerPathwayAssignmentService {

    List<CareerPathwayAssignmentOverviewDto> getOverview();

    void assign(AssignCareerPathwayRequestDto req, UUID userId);

    void update(AssignCareerPathwayRequestDto req, UUID userId);

    void delete(Long careerPathwayId, UUID userId);

    void bulkDelete(Set<Long> careerPathwayIds, UUID userId);

    ByteArrayResource exportData() throws IOException;

    void importData(MultipartFile file, UUID userId) throws IOException;
}
