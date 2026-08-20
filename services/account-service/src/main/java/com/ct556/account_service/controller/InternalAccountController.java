package com.ct556.account_service.controller;

import com.ct556.account_service.dto.InternalAccountResponse;
import com.ct556.account_service.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/accounts")
public class InternalAccountController {

    private final AccountService accountService;

    public InternalAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{accountId}")
    public InternalAccountResponse getInternalAccount(@PathVariable UUID accountId) {
        return accountService.getInternalById(accountId);
    }
}
