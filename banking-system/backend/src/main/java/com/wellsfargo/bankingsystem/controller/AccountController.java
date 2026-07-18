package com.wellsfargo.bankingsystem.controller;

import com.wellsfargo.bankingsystem.dto.AccountRequest;
import com.wellsfargo.bankingsystem.model.Account;
import com.wellsfargo.bankingsystem.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<Account> open(@Valid @RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.openAccount(request));
    }

    @GetMapping
    public List<Account> getAll() {
        return accountService.getAll();
    }

    @GetMapping("/{accountNumber}")
    public Account getByNumber(@PathVariable String accountNumber) {
        return accountService.getByAccountNumber(accountNumber);
    }

    @GetMapping("/customer/{customerId}")
    public List<Account> getByCustomer(@PathVariable Long customerId) {
        return accountService.getByCustomer(customerId);
    }

    @PostMapping("/{accountNumber}/close")
    public ResponseEntity<Void> close(@PathVariable String accountNumber) {
        accountService.closeAccount(accountNumber);
        return ResponseEntity.noContent().build();
    }
}
