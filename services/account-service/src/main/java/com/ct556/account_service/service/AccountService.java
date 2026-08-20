package com.ct556.account_service.service;

import com.ct556.account_service.client.AuditGateway;
import com.ct556.account_service.domain.Account;
import com.ct556.account_service.domain.AccountRole;
import com.ct556.account_service.domain.AccountStatus;
import com.ct556.account_service.dto.*;
import com.ct556.account_service.repository.AccountRepository;
import com.ct556.account_service.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountService {

    private static final long MAX_SIGNATURE_IMAGE_BYTES = 1024 * 1024;
    private static final int DEFAULT_SIGNATURE_BOX_X_PERCENT = 62;
    private static final int DEFAULT_SIGNATURE_BOX_Y_PERCENT = 4;
    private static final int DEFAULT_SIGNATURE_BOX_WIDTH_PERCENT = 34;
    private static final int DEFAULT_SIGNATURE_BOX_HEIGHT_PERCENT = 16;

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditGateway auditGateway;

    public AccountService(AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          AuditGateway auditGateway) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditGateway = auditGateway;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (accountRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (accountRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        LocalDateTime now = LocalDateTime.now();

        Account account = Account.builder()
                .id(UUID.randomUUID())
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(AccountRole.USER)
                .status(AccountStatus.PENDING)
                .signatureImageOnly(false)
                .signatureBoxXPercent(DEFAULT_SIGNATURE_BOX_X_PERCENT)
                .signatureBoxYPercent(DEFAULT_SIGNATURE_BOX_Y_PERCENT)
                .signatureBoxWidthPercent(DEFAULT_SIGNATURE_BOX_WIDTH_PERCENT)
                .signatureBoxHeightPercent(DEFAULT_SIGNATURE_BOX_HEIGHT_PERCENT)
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(account);

        publishAudit(
                "ACCOUNT_REGISTERED",
                account.getId(),
                account.getRole().name(),
                account.getId(),
                "ACCOUNT",
                account.getId(),
                "Account registered",
                Map.of(
                        "username", account.getUsername(),
                        "email", account.getEmail(),
                        "status", account.getStatus().name()
                )
        );

        return new RegisterResponse(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.getRole().name(),
                account.getStatus().name()
        );
    }

    public LoginResponse login(LoginRequest request, long expiresInMinutes) {
        Account account = accountRepository.findByUsername(request.usernameOrEmail())
                .or(() -> accountRepository.findByEmail(request.usernameOrEmail()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (account.getStatus() == AccountStatus.PENDING) {
            throw new IllegalStateException("Account is not activated");
        }

        if (account.getStatus() == AccountStatus.LOCKED) {
            throw new IllegalStateException("Account is locked");
        }

        if (account.getStatus() == AccountStatus.DISABLED) {
            throw new IllegalStateException("Account is disabled");
        }

        String token = jwtService.generateAccessToken(account);

        publishAudit(
                "ACCOUNT_LOGGED_IN",
                account.getId(),
                account.getRole().name(),
                account.getId(),
                "ACCOUNT",
                account.getId(),
                "Account logged in",
                Map.of(
                        "username", account.getUsername(),
                        "status", account.getStatus().name()
                )
        );

        return new LoginResponse(token, "Bearer", expiresInMinutes);
    }

    @Transactional
    public AccountResponse activate(UUID accountId, String actorUsername) {
        Account account = getAccount(accountId);
        ensureTransition(account, AccountStatus.PENDING, "Only PENDING accounts can be activated");
        account.setStatus(AccountStatus.ACTIVE);
        account.setUpdatedAt(LocalDateTime.now());
        Account saved = accountRepository.save(account);
        publishAdminAudit("ACCOUNT_ACTIVATED", actorUsername, saved, Map.of("status", saved.getStatus().name()));
        return toResponse(saved);
    }

    @Transactional
    public AccountResponse lock(UUID accountId, String actorUsername) {
        ensureAdminIsNotManagingSelf(accountId, actorUsername, "lock");
        Account account = getAccount(accountId);
        ensureTransition(account, AccountStatus.ACTIVE, "Only ACTIVE accounts can be locked");
        account.setStatus(AccountStatus.LOCKED);
        account.setUpdatedAt(LocalDateTime.now());
        Account saved = accountRepository.save(account);
        publishAdminAudit("ACCOUNT_LOCKED", actorUsername, saved, Map.of("status", saved.getStatus().name()));
        return toResponse(saved);
    }

    @Transactional
    public AccountResponse unlock(UUID accountId, String actorUsername) {
        Account account = getAccount(accountId);
        ensureTransition(account, AccountStatus.LOCKED, "Only LOCKED accounts can be unlocked");
        account.setStatus(AccountStatus.ACTIVE);
        account.setUpdatedAt(LocalDateTime.now());
        Account saved = accountRepository.save(account);
        publishAdminAudit("ACCOUNT_UNLOCKED", actorUsername, saved, Map.of("status", saved.getStatus().name()));
        return toResponse(saved);
    }

    @Transactional
    public AccountResponse disable(UUID accountId, String actorUsername) {
        ensureAdminIsNotManagingSelf(accountId, actorUsername, "disable");
        Account account = getAccount(accountId);
        if (account.getStatus() == AccountStatus.DISABLED) {
            throw new IllegalStateException("Account is already disabled");
        }
        account.setStatus(AccountStatus.DISABLED);
        account.setUpdatedAt(LocalDateTime.now());
        Account saved = accountRepository.save(account);
        publishAdminAudit("ACCOUNT_DISABLED", actorUsername, saved, Map.of("status", saved.getStatus().name()));
        return toResponse(saved);
    }

    @Transactional
    public AccountResponse restore(UUID accountId, String actorUsername) {
        Account account = getAccount(accountId);
        ensureTransition(account, AccountStatus.DISABLED, "Only DISABLED accounts can be restored");
        account.setStatus(AccountStatus.ACTIVE);
        account.setUpdatedAt(LocalDateTime.now());
        Account saved = accountRepository.save(account);
        publishAdminAudit("ACCOUNT_RESTORED", actorUsername, saved, Map.of("status", saved.getStatus().name()));
        return toResponse(saved);
    }

    @Transactional
    public AccountResponse changeRole(UUID accountId, String role, String actorUsername) {
        Account account = getAccount(accountId);
        account.setRole(AccountRole.valueOf(role.toUpperCase()));
        account.setUpdatedAt(LocalDateTime.now());
        Account saved = accountRepository.save(account);
        publishAdminAudit("ACCOUNT_ROLE_CHANGED", actorUsername, saved, Map.of(
                "role", saved.getRole().name(),
                "status", saved.getStatus().name()
        ));
        return toResponse(saved);
    }

    public AccountResponse getById(UUID accountId) {
        return toResponse(getAccount(accountId));
    }

    public AccountResponse getCurrentAccount(String principal) {
        Account account = accountRepository.findByUsername(principal)
                .or(() -> accountRepository.findByEmail(principal))
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return toResponse(account);
    }

    @Transactional
    public AccountResponse updateSignatureImage(String principal, MultipartFile file) {
        Account account = accountRepository.findByUsername(principal)
                .or(() -> accountRepository.findByEmail(principal))
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        validateSignatureImage(file);

        try {
            account.setSignatureImage(file.getBytes());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read uploaded signature image", ex);
        }

        account.setSignatureImageContentType(normalizeSignatureImageContentType(file.getContentType()));
        account.setSignatureImageUpdatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        Account saved = accountRepository.save(account);
        publishAudit(
                "ACCOUNT_SIGNATURE_IMAGE_UPDATED",
                saved.getId(),
                saved.getRole().name(),
                saved.getId(),
                "ACCOUNT",
                saved.getId(),
                "Signature image updated",
                Map.of(
                        "contentType", saved.getSignatureImageContentType(),
                        "configured", true
                )
        );
        return toResponse(saved);
    }

    public DownloadedSignatureImage getCurrentSignatureImage(String principal) {
        Account account = accountRepository.findByUsername(principal)
                .or(() -> accountRepository.findByEmail(principal))
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (account.getSignatureImage() == null || account.getSignatureImage().length == 0) {
            throw new IllegalArgumentException("Signature image is not configured");
        }

        return new DownloadedSignatureImage(
                normalizeSignatureImageContentType(account.getSignatureImageContentType()),
                account.getSignatureImage()
        );
    }

    @Transactional
    public AccountResponse updateSignatureAppearanceSettings(String principal, SignatureAppearanceSettingsRequest request) {
        Account account = accountRepository.findByUsername(principal)
                .or(() -> accountRepository.findByEmail(principal))
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        validateSignatureAppearanceSettings(request);

        account.setSignatureImageOnly(request.imageOnly());
        account.setSignatureBoxXPercent(request.xPercent());
        account.setSignatureBoxYPercent(request.yPercent());
        account.setSignatureBoxWidthPercent(request.widthPercent());
        account.setSignatureBoxHeightPercent(request.heightPercent());
        account.setUpdatedAt(LocalDateTime.now());

        Account saved = accountRepository.save(account);
        publishAudit(
                "ACCOUNT_SIGNATURE_APPEARANCE_UPDATED",
                saved.getId(),
                saved.getRole().name(),
                saved.getId(),
                "ACCOUNT",
                saved.getId(),
                "Signature appearance settings updated",
                Map.of(
                        "imageOnly", saved.isSignatureImageOnly(),
                        "xPercent", saved.getSignatureBoxXPercent(),
                        "yPercent", saved.getSignatureBoxYPercent(),
                        "widthPercent", saved.getSignatureBoxWidthPercent(),
                        "heightPercent", saved.getSignatureBoxHeightPercent()
                )
        );
        return toResponse(saved);
    }

    @Transactional
    public AccountResponse clearSignatureImage(String principal) {
        Account account = accountRepository.findByUsername(principal)
                .or(() -> accountRepository.findByEmail(principal))
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setSignatureImage(null);
        account.setSignatureImageContentType(null);
        account.setSignatureImageUpdatedAt(null);
        account.setUpdatedAt(LocalDateTime.now());

        Account saved = accountRepository.save(account);
        publishAudit(
                "ACCOUNT_SIGNATURE_IMAGE_CLEARED",
                saved.getId(),
                saved.getRole().name(),
                saved.getId(),
                "ACCOUNT",
                saved.getId(),
                "Signature image cleared",
                Map.of("configured", false)
        );
        return toResponse(saved);
    }

    public List<AccountResponse> listAccounts(String role, String status, String q) {
        String normalizedRole = role == null ? null : role.trim().toUpperCase(Locale.ROOT);
        String normalizedStatus = status == null ? null : status.trim().toUpperCase(Locale.ROOT);
        String normalizedQuery = q == null ? null : q.trim().toLowerCase(Locale.ROOT);

        return accountRepository.findAll().stream()
                .filter(account -> normalizedRole == null || account.getRole().name().equals(normalizedRole))
                .filter(account -> normalizedStatus == null || account.getStatus().name().equals(normalizedStatus))
                .filter(account -> normalizedQuery == null
                        || account.getUsername().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || account.getEmail().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparing(Account::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    public List<AccountResponse> listActiveUsers() {
        return accountRepository.findAll().stream()
                .filter(account -> account.getRole() == AccountRole.USER)
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
                .sorted(Comparator.comparing(Account::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    public InternalAccountResponse getInternalById(UUID accountId) {
        Account account = getAccount(accountId);
        return new InternalAccountResponse(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.getRole().name(),
                account.getStatus().name(),
                account.getSignatureImage() == null ? null : Base64.getEncoder().encodeToString(account.getSignatureImage()),
                account.getSignatureImageContentType(),
                account.isSignatureImageOnly(),
                account.getSignatureBoxXPercent(),
                account.getSignatureBoxYPercent(),
                account.getSignatureBoxWidthPercent(),
                account.getSignatureBoxHeightPercent()
        );
    }

    private Account getAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    private void ensureAdminIsNotManagingSelf(UUID targetAccountId, String actorUsername, String action) {
        Account actor = accountRepository.findByUsername(actorUsername)
                .or(() -> accountRepository.findByEmail(actorUsername))
                .orElseThrow(() -> new IllegalArgumentException("Actor account not found"));

        if (actor.getId().equals(targetAccountId)) {
            throw new IllegalStateException("Admin cannot " + action + " their own account");
        }
    }

    private void ensureTransition(Account account, AccountStatus expectedCurrentStatus, String message) {
        if (account.getStatus() != expectedCurrentStatus) {
            throw new IllegalStateException(message);
        }
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.getRole().name(),
                account.getStatus().name(),
                account.getSignatureImage() != null && account.getSignatureImage().length > 0,
                account.isSignatureImageOnly(),
                account.getSignatureBoxXPercent(),
                account.getSignatureBoxYPercent(),
                account.getSignatureBoxWidthPercent(),
                account.getSignatureBoxHeightPercent()
        );
    }

    private void validateSignatureImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Signature image file is required");
        }
        if (file.getSize() > MAX_SIGNATURE_IMAGE_BYTES) {
            throw new IllegalArgumentException("Signature image must not exceed 1MB");
        }

        String contentType = normalizeSignatureImageContentType(file.getContentType());
        if (!"image/png".equals(contentType) && !"image/jpeg".equals(contentType)) {
            throw new IllegalArgumentException("Only PNG or JPEG signature images are supported");
        }
    }

    private String normalizeSignatureImageContentType(String contentType) {
        String normalized = StringUtils.hasText(contentType) ? contentType.trim().toLowerCase(Locale.ROOT) : "image/png";
        if ("image/jpg".equals(normalized)) {
            return "image/jpeg";
        }
        return normalized;
    }

    private void validateSignatureAppearanceSettings(SignatureAppearanceSettingsRequest request) {
        if (request.xPercent() + request.widthPercent() > 100) {
            throw new IllegalArgumentException("X + width must not exceed 100%");
        }
        if (request.yPercent() + request.heightPercent() > 100) {
            throw new IllegalArgumentException("Y + height must not exceed 100%");
        }
    }

    private void publishAdminAudit(String eventType, String actorUsername, Account targetAccount, Map<String, Object> payload) {
        Account adminActor = accountRepository.findByUsername(actorUsername)
                .or(() -> accountRepository.findByEmail(actorUsername))
                .orElse(null);

        publishAudit(
                eventType,
                adminActor == null ? null : adminActor.getId(),
                adminActor == null ? "ADMIN" : adminActor.getRole().name(),
                targetAccount.getId(),
                "ACCOUNT",
                targetAccount.getId(),
                "Admin account action executed",
                payload
        );
    }

    private void publishAudit(String eventType,
                              UUID actorId,
                              String actorRole,
                              UUID targetId,
                              String resourceType,
                              UUID resourceId,
                              String summary,
                              Map<String, Object> payload) {
        auditGateway.publishEvent(
                eventType,
                actorId,
                actorRole == null ? null : actorRole.toUpperCase(Locale.ROOT),
                targetId,
                resourceType,
                resourceId,
                resourceId == null ? null : resourceId.toString(),
                summary,
                new LinkedHashMap<>(payload)
        );
    }
}
