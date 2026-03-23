package com.tbm.careerpathlearning.service;

import com.tbm.careerpathlearning.dto.*;
import com.tbm.careerpathlearning.enums.OtpPurpose;
import com.tbm.careerpathlearning.enums.StaffAccountStatus;
import com.tbm.careerpathlearning.exception.BadRequestException;
import com.tbm.careerpathlearning.exception.ForbiddenRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AuthServiceImpl implements AuthService {

    @Value("${account.max.login.failed.attempts}")
    private int MAX_FAILED_ATTEMPTS;

    @Value("${account.max.reset.password.attempts}")
    private int MAX_RESET_PASSWORD_ATTEMPTS;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private EmailService emailService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private RoleAuthorityService roleAuthorityService;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private StaffLoginAuditService staffLoginAuditService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private StaffRefreshTokenService staffRefreshTokenService;

    private static final int ONE_DAY_IN_HOURS = 24;
    private static final int MINUTE_BEFORE_OTP_RESENT = 1;
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$";

    private static final String FORBIDDEN_ERR_MSG_CODE = "forbidden.request.err.msg";
    private static final String INVALID_REQUEST_BODY_ERR_MSG_CODE = "invalid.request.body.err.msg";
    private static final String EMAIL_NOT_REGISTERED_ERR_TITLE_CODE = "email.not.registered.err.title";
    private static final String EMAIL_NOT_REGISTERED_ERR_MSG_CODE = "email.not.registered.err.msg";
    private static final String ACCOUNT_BLOCKED_ERR_TITLE_CODE = "account.blocked.err.title";
    private static final String ACCOUNT_BLOCKED_ERR_MSG_CODE = "account.blocked.err.msg";
    private static final String OTP_EXCEED_DAY_LIMIT_ERR_TITLE_CODE = "otp.request.exceed.day.limit.err.title";
    private static final String OTP_EXCEED_DAY_LIMIT_ERR_MSG_CODE = "otp.request.exceed.day.limit.err.msg";
    private static final String OTP_EXCEED_MINUTE_LIMIT_ERR_TITLE_CODE = "otp.request.exceed.minute.limit.err.title";
    private static final String OTP_EXCEED_MINUTE_LIMIT_ERR_MSG_CODE = "otp.request.exceed.minute.limit.err.msg";
    private static final String INVALID_PASSWORD_FORMAT_ERR_TITLE_CODE = "invalid.password.format.err.title";
    private static final String INVALID_PASSWORD_FORMAT_ERR_MSG_CODE = "invalid.password.format.err.msg";
    private static final String FIRST_TIME_LOGIN_ERR_TITLE_CODE = "first.time.login.err.title";
    private static final String FIRST_TIME_LOGIN_ERR_MSG_CODE = "first.time.login.err.msg";
    private static final String NOT_FIRST_TIME_LOGIN_ERR_TITLE_CODE = "not.first.time.login.err.title";
    private static final String NOT_FIRST_TIME_LOGIN_ERR_MSG_CODE = "not.first.time.login.err.msg";

    private static final String LOGIN_OPERATION = "Login";
    private static final String FORGOT_PASSWORD_OPERATION = "Forgot Password";
    private static final String RESET_PASSWORD_OPERATION = "Reset Password";
    private static final String FIRST_TIME_LOGIN_OPERATION = "First Time Login";

    @Override
    public LoginResult login(LoginRequest req) throws Exception {
        if (validationService.isNullOrBlank(req.getEmail()) || validationService.isNullOrBlank(req.getPassword())) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{LOGIN_OPERATION}, Locale.getDefault()));
        }

        String email = req.getEmail().trim().toLowerCase();
        String password = req.getPassword();

        StaffDto staffDto = staffService.findByIsDeletedIsFalseAndEmail(email)
                .orElseThrow(() -> new BadRequestException(
                messageSource.getMessage(EMAIL_NOT_REGISTERED_ERR_TITLE_CODE, null, Locale.getDefault()),
                messageSource.getMessage(EMAIL_NOT_REGISTERED_ERR_MSG_CODE, null, Locale.getDefault())));

        if (staffDto.isFirstLogin()) {
            throw new BadRequestException(
                    messageSource.getMessage(FIRST_TIME_LOGIN_ERR_TITLE_CODE, null, Locale.getDefault()),
                    messageSource.getMessage(FIRST_TIME_LOGIN_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        StaffLoginAuditDto staffLoginAuditDto = staffLoginAuditService.getStaffLoginAuditById(staffDto.getId());

        if (staffDto.getAccountStatus() != StaffAccountStatus.ACTIVE || staffLoginAuditDto.getLoginFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new ForbiddenRequestException(
                    messageSource.getMessage(ACCOUNT_BLOCKED_ERR_TITLE_CODE, null, Locale.getDefault()),
                    messageSource.getMessage(ACCOUNT_BLOCKED_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        if (!authenticationService.loginWithEmailAndPassword(staffLoginAuditDto, staffDto, password)) {
            throw new ForbiddenRequestException(messageSource.getMessage(FORBIDDEN_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        UUID userId = staffDto.getId();
        List<String> roles = getGrantedAuthoritiesList(staffDto).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = tokenService.generateAccessToken(userId, roles);
        String refreshToken = authenticationService.createAndSaveRefreshToken(userId);

        staffLoginAuditDto.setLoginFailedAttempts(0);
        staffLoginAuditDto.setLastLoginAt(OffsetDateTime.now());
        staffLoginAuditService.updateStaffLoginAudit(userId, staffLoginAuditDto);

        return new LoginResult(userId, roles, accessToken, refreshToken);
    }

    @Override
    public RefreshResult refresh(String refreshToken) throws Exception {
        if (refreshToken == null) {
            throw new ForbiddenRequestException(messageSource.getMessage(FORBIDDEN_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        StaffRefreshTokenDto tokenDto = staffRefreshTokenService.getStaffRefreshTokenByToken(refreshToken)
                .filter(t -> !t.getExpiresAt().isBefore(OffsetDateTime.now()))
                .orElseThrow(() -> new ForbiddenRequestException(messageSource.getMessage(FORBIDDEN_ERR_MSG_CODE, null, Locale.getDefault())));

        UUID userId = tokenDto.getStaffId();
        String newRefreshToken = authenticationService.createAndSaveRefreshToken(userId);

        StaffDto staffDto = staffService.findById(userId);
        List<String> roles = getGrantedAuthoritiesList(staffDto).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = tokenService.generateAccessToken(userId, roles);

        return new RefreshResult(userId, roles, accessToken, newRefreshToken);
    }

    @Override
    public void forgotPassword(String email) throws Exception {
        if (validationService.isNullOrBlank(email)) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{FORGOT_PASSWORD_OPERATION}, Locale.getDefault()));
        }

        email = email.trim().toLowerCase();

        StaffDto staffDto = staffService.findByIsDeletedIsFalseAndEmail(email)
                .orElseThrow(() -> new BadRequestException(
                messageSource.getMessage(EMAIL_NOT_REGISTERED_ERR_TITLE_CODE, null, Locale.getDefault()),
                messageSource.getMessage(EMAIL_NOT_REGISTERED_ERR_MSG_CODE, null, Locale.getDefault())));

        if (staffDto.isFirstLogin()) {
            firstTimeLogin(email);
            return;
        }

        StaffLoginAuditDto staffLoginAuditDto = staffLoginAuditService.getStaffLoginAuditById(staffDto.getId());

        if (staffDto.getAccountStatus() != StaffAccountStatus.ACTIVE || staffLoginAuditDto.getLoginFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new ForbiddenRequestException(
                    messageSource.getMessage(ACCOUNT_BLOCKED_ERR_TITLE_CODE, null, Locale.getDefault()),
                    messageSource.getMessage(ACCOUNT_BLOCKED_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        checkOtpDayLimit(staffLoginAuditDto);
        checkOtpMinuteLimit(staffLoginAuditDto);

        String otp = authenticationService.generateOtp(staffDto, OtpPurpose.PASSWORD_RESET);
        emailService.sendPasswordResetEmail(email, otp, Locale.getDefault());

        staffLoginAuditDto.setForgotPasswordAttempts(staffLoginAuditDto.getForgotPasswordAttempts() + 1);
        staffLoginAuditDto.setLastForgotPasswordAt(OffsetDateTime.now());
        staffLoginAuditService.updateStaffLoginAudit(staffDto.getId(), staffLoginAuditDto);
    }

    @Override
    public void resetPassword(ResetRequest req) throws Exception {
        if (validationService.isNullOrBlank(req.getEmail()) || validationService.isNullOrBlank(req.getOtp())
                || validationService.isNullOrBlank(req.getPassword())) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{RESET_PASSWORD_OPERATION}, Locale.getDefault()));
        }

        String email = req.getEmail().trim().toLowerCase();
        String otp = req.getOtp().trim();
        String newPassword = req.getPassword();

        if (!newPassword.matches(PASSWORD_REGEX)) {
            throw new BadRequestException(
                    messageSource.getMessage(INVALID_PASSWORD_FORMAT_ERR_TITLE_CODE, null, Locale.getDefault()),
                    messageSource.getMessage(INVALID_PASSWORD_FORMAT_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        StaffDto staffDto = staffService.findByIsDeletedIsFalseAndEmail(email)
                .orElseThrow(() -> new BadRequestException(
                messageSource.getMessage(EMAIL_NOT_REGISTERED_ERR_TITLE_CODE, null, Locale.getDefault()),
                messageSource.getMessage(EMAIL_NOT_REGISTERED_ERR_MSG_CODE, null, Locale.getDefault())));

        StaffLoginAuditDto staffLoginAuditDto = staffLoginAuditService.getStaffLoginAuditById(staffDto.getId());

        if (staffDto.getAccountStatus() != StaffAccountStatus.ACTIVE || staffLoginAuditDto.getLoginFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new ForbiddenRequestException(
                    messageSource.getMessage(ACCOUNT_BLOCKED_ERR_TITLE_CODE, null, Locale.getDefault()),
                    messageSource.getMessage(ACCOUNT_BLOCKED_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        OtpPurpose otpPurpose = staffDto.isFirstLogin() ? OtpPurpose.ACCOUNT_ACTIVATION : OtpPurpose.PASSWORD_RESET;
        if (!authenticationService.validateOtp(staffDto, otp, otpPurpose)) {
            throw new ForbiddenRequestException(messageSource.getMessage(FORBIDDEN_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        UUID userId = staffDto.getId();
        authenticationService.updateUserPasswordByUserId(staffDto, newPassword, userId);

        if (staffDto.isFirstLogin()) {
            staffDto.setFirstLogin(false);
            staffService.update(userId, staffDto);
        }

        staffLoginAuditDto.setLastResetPasswordAt(OffsetDateTime.now());
        staffLoginAuditService.updateStaffLoginAudit(userId, staffLoginAuditDto);
    }

    @Override
    public void firstTimeLogin(String email) throws Exception {
        if (validationService.isNullOrBlank(email)) {
            throw new BadRequestException(messageSource.getMessage(INVALID_REQUEST_BODY_ERR_MSG_CODE, new String[]{FIRST_TIME_LOGIN_OPERATION}, Locale.getDefault()));
        }

        email = email.trim().toLowerCase();

        StaffDto staffDto = staffService.findByIsDeletedIsFalseAndEmail(email)
                .orElseThrow(() -> new BadRequestException(
                messageSource.getMessage(EMAIL_NOT_REGISTERED_ERR_TITLE_CODE, null, Locale.getDefault()),
                messageSource.getMessage(EMAIL_NOT_REGISTERED_ERR_MSG_CODE, null, Locale.getDefault())));

        if (!staffDto.isFirstLogin()) {
            throw new BadRequestException(
                    messageSource.getMessage(NOT_FIRST_TIME_LOGIN_ERR_TITLE_CODE, null, Locale.getDefault()),
                    messageSource.getMessage(NOT_FIRST_TIME_LOGIN_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        StaffRefreshTokenDto staffRefreshTokenDto = new StaffRefreshTokenDto();
        staffRefreshTokenDto.setStaffId(staffDto.getId());
        staffRefreshTokenService.createStaffRefreshToken(staffRefreshTokenDto);

        StaffLoginAuditDto staffLoginAuditDtoToBeCreated = new StaffLoginAuditDto();
        staffLoginAuditDtoToBeCreated.setStaffId(staffDto.getId());
        StaffLoginAuditDto staffLoginAuditDto = staffLoginAuditService.createStaffLoginAudit(staffLoginAuditDtoToBeCreated);

        if (staffDto.getAccountStatus() != StaffAccountStatus.ACTIVE || staffLoginAuditDto.getLoginFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new ForbiddenRequestException(
                    messageSource.getMessage(ACCOUNT_BLOCKED_ERR_TITLE_CODE, null, Locale.getDefault()),
                    messageSource.getMessage(ACCOUNT_BLOCKED_ERR_MSG_CODE, null, Locale.getDefault()));
        }

        if (staffLoginAuditDto.getLastForgotPasswordAt() != null && staffLoginAuditDto.getForgotPasswordAttempts() >= MAX_RESET_PASSWORD_ATTEMPTS) {
            long hours = ChronoUnit.HOURS.between(staffLoginAuditDto.getLastForgotPasswordAt(), OffsetDateTime.now());
            if (hours >= ONE_DAY_IN_HOURS) {
                staffLoginAuditDto.setForgotPasswordAttempts(0);
            } else {
                throw new ForbiddenRequestException(
                        messageSource.getMessage(OTP_EXCEED_DAY_LIMIT_ERR_TITLE_CODE, null, Locale.getDefault()),
                        messageSource.getMessage(OTP_EXCEED_DAY_LIMIT_ERR_MSG_CODE, null, Locale.getDefault()));
            }
        }

        checkOtpMinuteLimit(staffLoginAuditDto);

        String otp = authenticationService.generateOtp(staffDto, OtpPurpose.ACCOUNT_ACTIVATION);
        emailService.sendAccountActivationEmail(email, otp, Locale.getDefault());

        staffLoginAuditDto.setForgotPasswordAttempts(staffLoginAuditDto.getForgotPasswordAttempts() + 1);
        staffLoginAuditDto.setLastForgotPasswordAt(OffsetDateTime.now());
        staffLoginAuditService.updateStaffLoginAudit(staffDto.getId(), staffLoginAuditDto);
    }

    private void checkOtpDayLimit(StaffLoginAuditDto staffLoginAuditDto) {
        if (staffLoginAuditDto.getLastForgotPasswordAt() == null) {
            return;
        }
        long hours = ChronoUnit.HOURS.between(staffLoginAuditDto.getLastForgotPasswordAt(), OffsetDateTime.now());
        if (hours >= ONE_DAY_IN_HOURS) {
            staffLoginAuditDto.setForgotPasswordAttempts(0);
        } else if (staffLoginAuditDto.getForgotPasswordAttempts() >= MAX_RESET_PASSWORD_ATTEMPTS) {
            throw new ForbiddenRequestException(
                    messageSource.getMessage(OTP_EXCEED_DAY_LIMIT_ERR_TITLE_CODE, null, Locale.getDefault()),
                    messageSource.getMessage(OTP_EXCEED_DAY_LIMIT_ERR_MSG_CODE, null, Locale.getDefault()));
        }
    }

    private void checkOtpMinuteLimit(StaffLoginAuditDto staffLoginAuditDto) {
        if (staffLoginAuditDto.getLastForgotPasswordAt() == null) {
            return;
        }
        long minutes = ChronoUnit.MINUTES.between(staffLoginAuditDto.getLastForgotPasswordAt(), OffsetDateTime.now());
        if (minutes <= MINUTE_BEFORE_OTP_RESENT) {
            throw new ForbiddenRequestException(
                    messageSource.getMessage(OTP_EXCEED_MINUTE_LIMIT_ERR_TITLE_CODE, null, Locale.getDefault()),
                    messageSource.getMessage(OTP_EXCEED_MINUTE_LIMIT_ERR_MSG_CODE, null, Locale.getDefault()));
        }
    }

    private List<GrantedAuthority> getGrantedAuthoritiesList(StaffDto staffDto) {
        RoleDto role = staffDto.getRole();
        List<RoleAuthorityDto> roleAuthorityDtoList = roleAuthorityService.getAllByRoleId(role.getId());
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (RoleAuthorityDto roleAuthorityDto : roleAuthorityDtoList) {
            AuthorityDto authorityDto = authorityService.findById(roleAuthorityDto.getAuthority().getId());
            authorities.add(new SimpleGrantedAuthority(authorityDto.getName().getAuthorityName()));
        }
        return authorities;
    }
}
