package com.tbm.careerpathlearning.service;

import com.tbm.careerpathlearning.dto.*;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

public interface AccessControlService {

    Map<String, Object> getOverview();

    void grantAccess(GrantAccessDto grantAccessDto, UUID userId);

    void editGrantedAccess(GrantAccessDto grantAccessDto, UUID userId);

    void deleteRoleAuthority(Long orgChartId, Long roleId);

    void bulkDeleteRoleAuthority(List<Long> selectedRoleIds);

    ByteArrayResource exportData() throws IOException;

    void importData(MultipartFile file, UUID userId) throws IOException;

}
