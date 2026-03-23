package com.tbm.careerpathlearning.service;

import com.tbm.careerpathlearning.dto.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public interface AuthService {

    record LoginResult(UUID userId, List<String> roles, String accessToken, String refreshToken) {

    }

    record RefreshResult(UUID userId, List<String> roles, String accessToken, String newRefreshToken) {

    }

    LoginResult login(LoginRequest req) throws Exception;

    RefreshResult refresh(String refreshToken) throws Exception;

    void forgotPassword(String email) throws Exception;

    void resetPassword(ResetRequest req) throws Exception;

    void firstTimeLogin(String email) throws Exception;
}
