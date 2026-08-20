package com.ct556.account_service.controller;

import com.ct556.account_service.config.JwtProperties;
import com.ct556.account_service.dto.LoginRequest;
import com.ct556.account_service.dto.LoginResponse;
import com.ct556.account_service.dto.RegisterRequest;
import com.ct556.account_service.dto.RegisterResponse;
import com.ct556.account_service.dto.AccountResponse;
import com.ct556.account_service.dto.SignatureAppearanceSettingsRequest;
import com.ct556.account_service.service.DownloadedSignatureImage;
import com.ct556.account_service.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AccountService accountService;
    private final JwtProperties jwtProperties;
    public AuthController(AccountService accountService,
                          JwtProperties jwtProperties) {
        this.accountService = accountService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return accountService.register(request);
    }
    
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return accountService.login(request, jwtProperties.accessTokenExpirationMinutes());
    }

    @GetMapping("/me")
    public AccountResponse me(Authentication authentication) {
        return accountService.getCurrentAccount(authentication.getName());
    }

    @PostMapping(value = "/me/signature-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AccountResponse uploadSignatureImage(@RequestPart("file") MultipartFile file,
                                                Authentication authentication) {
        return accountService.updateSignatureImage(authentication.getName(), file);
    }

    @GetMapping(value = "/me/signature-image")
    public ResponseEntity<byte[]> getSignatureImage(Authentication authentication) {
        DownloadedSignatureImage downloadedSignatureImage = accountService.getCurrentSignatureImage(authentication.getName());
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(downloadedSignatureImage.contentType());
        } catch (Exception ex) {
            mediaType = MediaType.IMAGE_PNG;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(downloadedSignatureImage.content());
    }

    @DeleteMapping("/me/signature-image")
    public AccountResponse deleteSignatureImage(Authentication authentication) {
        return accountService.clearSignatureImage(authentication.getName());
    }

    @PatchMapping("/me/signature-appearance")
    public AccountResponse updateSignatureAppearance(@Valid @RequestBody SignatureAppearanceSettingsRequest request,
                                                     Authentication authentication) {
        return accountService.updateSignatureAppearanceSettings(authentication.getName(), request);
    }

    @GetMapping("/active-users")
    public List<AccountResponse> activeUsers() {
        return accountService.listActiveUsers();
    }
}
