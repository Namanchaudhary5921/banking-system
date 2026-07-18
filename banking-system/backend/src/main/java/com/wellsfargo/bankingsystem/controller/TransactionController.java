package com.wellsfargo.bankingsystem.controller;

import com.wellsfargo.bankingsystem.dto.DepositWithdrawRequest;
import com.wellsfargo.bankingsystem.dto.TransferRequest;
import com.wellsfargo.bankingsystem.model.Transaction;
import com.wellsfargo.bankingsystem.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public Transaction deposit(@Valid @RequestBody DepositWithdrawRequest request) {
        return transactionService.deposit(request.getAccountNumber(), request.getAmount());
    }

    @PostMapping("/withdraw")
    public Transaction withdraw(@Valid @RequestBody DepositWithdrawRequest request) {
        return transactionService.withdraw(request.getAccountNumber(), request.getAmount());
    }

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, String>> transfer(@Valid @RequestBody TransferRequest request) {
        transactionService.transfer(request.getFromAccountNumber(), request.getToAccountNumber(), request.getAmount());
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Transfer completed"));
    }

    @GetMapping("/account/{accountNumber}")
    public List<Transaction> history(@PathVariable String accountNumber) {
        return transactionService.getHistory(accountNumber);
    }

    @GetMapping("/flagged")
    public List<Transaction> flagged() {
        return transactionService.getFlaggedTransactions();
    }
}
