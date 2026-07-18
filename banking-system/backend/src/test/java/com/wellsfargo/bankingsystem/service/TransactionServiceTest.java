package com.wellsfargo.bankingsystem.service;

import com.wellsfargo.bankingsystem.exception.InsufficientFundsException;
import com.wellsfargo.bankingsystem.model.Account;
import com.wellsfargo.bankingsystem.model.AccountType;
import com.wellsfargo.bankingsystem.model.Customer;
import com.wellsfargo.bankingsystem.model.Transaction;
import com.wellsfargo.bankingsystem.repository.AccountRepository;
import com.wellsfargo.bankingsystem.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the core money-movement rules: deposits, withdrawals,
 * and insufficient-funds rejection. Uses Mockito to isolate the service
 * from the database (see test-plan.md for the full test matrix, including
 * the integration-level transfer atomicity tests).
 */
class TransactionServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private FraudDetectionService fraudDetectionService;
    @Mock private AuditService auditService;

    private TransactionService transactionService;

    private Account account;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transactionService = new TransactionService(accountRepository, transactionRepository, fraudDetectionService, auditService);

        Customer customer = new Customer("Jane", "Doe", "jane@example.com", "NID-1", "555-0100", "123 Main St");
        account = new Account("4000000001", customer, AccountType.CHECKING, new BigDecimal("500.00"), new BigDecimal("0.0010"));
        account.setId(1L);

        when(fraudDetectionService.evaluate(any(), any(), any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.findByAccountNumberForUpdate("4000000001")).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountIdAndTimestampAfter(any(), any())).thenReturn(Collections.emptyList());
    }

    @Test
    void deposit_increasesBalance() {
        Transaction txn = transactionService.deposit("4000000001", new BigDecimal("100.00"));

        assertEquals(new BigDecimal("600.00"), account.getBalance());
        assertEquals(new BigDecimal("600.00"), txn.getBalanceAfter());
        verify(accountRepository).save(account);
    }

    @Test
    void withdraw_decreasesBalance_whenFundsSufficient() {
        Transaction txn = transactionService.withdraw("4000000001", new BigDecimal("200.00"));

        assertEquals(new BigDecimal("300.00"), account.getBalance());
        assertEquals(new BigDecimal("300.00"), txn.getBalanceAfter());
    }

    @Test
    void withdraw_throwsInsufficientFunds_andLeavesBalanceUnchanged() {
        BigDecimal originalBalance = account.getBalance();

        assertThrows(InsufficientFundsException.class,
                () -> transactionService.withdraw("4000000001", new BigDecimal("999999.00")));

        // Balance must be untouched - this is what "atomic" means in practice.
        assertEquals(originalBalance, account.getBalance());
    }
}
