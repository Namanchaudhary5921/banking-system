package com.wellsfargo.bankingsystem.controller;

import com.wellsfargo.bankingsystem.exception.ResourceNotFoundException;
import com.wellsfargo.bankingsystem.model.Account;
import com.wellsfargo.bankingsystem.model.AppUser;
import com.wellsfargo.bankingsystem.model.Role;
import com.wellsfargo.bankingsystem.model.Transaction;
import com.wellsfargo.bankingsystem.security.CurrentUserService;
import com.wellsfargo.bankingsystem.service.AccountService;
import com.wellsfargo.bankingsystem.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUserService currentUserService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public MeController(CurrentUserService currentUserService, AccountService accountService,
                         TransactionService transactionService) {
        this.currentUserService = currentUserService;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping
    public Map<String, Object> me() {
        AppUser user = currentUserService.getCurrentAppUser();
        return Map.of(
                "username", user.getUsername(),
                "role", user.getRole(),
                "customerId", user.getCustomerId() == null ? "" : user.getCustomerId()
        );
    }

    @GetMapping("/accounts")
    public List<Account> myAccounts() {
        AppUser user = currentUserService.getCurrentAppUser();
        requireCustomer(user);
        return accountService.getByCustomer(user.getCustomerId());
    }

    @GetMapping("/transactions/{accountNumber}")
    public List<Transaction> myTransactions(@PathVariable String accountNumber) {
        AppUser user = currentUserService.getCurrentAppUser();
        requireCustomer(user);

        Account account = accountService.getByAccountNumber(accountNumber);
        if (!account.getCustomer().getId().equals(user.getCustomerId())) {
            throw new ResourceNotFoundException("Account not found: " + accountNumber);
        }
        return transactionService.getHistory(accountNumber);
    }

    private void requireCustomer(AppUser user) {
        if (user.getRole() != Role.CUSTOMER || user.getCustomerId() == null) {
            throw new ResourceNotFoundException("No customer profile linked to this login");
        }
    }
}
