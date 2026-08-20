package com.ct556.account_service.controller;

import com.ct556.account_service.dto.AccountResponse;
import com.ct556.account_service.dto.ChangeRoleRequest;
import com.ct556.account_service.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/accounts")
public class AdminAccountController {

    private final AccountService accountService;

    public AdminAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountResponse> list(@RequestParam(required = false) String role,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String q) {
        return accountService.listAccounts(role, status, q);
    }

    @PatchMapping("/{accountId}/activate")
    public AccountResponse activate(@PathVariable UUID accountId, Authentication authentication) {
        return accountService.activate(accountId, authentication.getName());
    }

    @PatchMapping("/{accountId}/lock")
    public AccountResponse lock(@PathVariable UUID accountId, Authentication authentication) {
        return accountService.lock(accountId, authentication.getName());
    }

    @PatchMapping("/{accountId}/unlock")
    public AccountResponse unlock(@PathVariable UUID accountId, Authentication authentication) {
        return accountService.unlock(accountId, authentication.getName());
    }

    @PatchMapping("/{accountId}/disable")
    public AccountResponse disable(@PathVariable UUID accountId, Authentication authentication) {
        return accountService.disable(accountId, authentication.getName());
    }

    @PatchMapping("/{accountId}/restore")
    public AccountResponse restore(@PathVariable UUID accountId, Authentication authentication) {
        return accountService.restore(accountId, authentication.getName());
    }

    @PatchMapping("/{accountId}/role")
    public AccountResponse changeRole(@PathVariable UUID accountId,
                                      @Valid @RequestBody ChangeRoleRequest request,
                                      Authentication authentication) {
        return accountService.changeRole(accountId, request.role(), authentication.getName());
    }

    @GetMapping("/{accountId}")
    public AccountResponse getById(@PathVariable UUID accountId) {
        return accountService.getById(accountId);
    }
}
