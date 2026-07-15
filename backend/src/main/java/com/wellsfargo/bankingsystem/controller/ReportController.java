package com.wellsfargo.bankingsystem.controller;

import com.wellsfargo.bankingsystem.model.AuditLog;
import com.wellsfargo.bankingsystem.repository.AccountRepository;
import com.wellsfargo.bankingsystem.repository.CustomerRepository;
import com.wellsfargo.bankingsystem.service.AuditService;
import com.wellsfargo.bankingsystem.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;
    private final TransactionService transactionService;

    public ReportController(AccountRepository accountRepository, CustomerRepository customerRepository,
                             AuditService auditService, TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
        this.transactionService = transactionService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        BigDecimal totalAssets = accountRepository.findAll().stream()
                .map(a -> a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "totalCustomers", customerRepository.count(),
                "totalAccounts", accountRepository.count(),
                "totalAssetsUnderManagement", totalAssets,
                "flaggedTransactionCount", transactionService.getFlaggedTransactions().size()
        );
    }

    @GetMapping("/audit-log")
    public List<AuditLog> auditLog() {
        return auditService.getAllLogs();
    }
}
