package com.wellsfargo.bankingsystem.service;

import com.wellsfargo.bankingsystem.dto.AccountRequest;
import com.wellsfargo.bankingsystem.model.Account;
import com.wellsfargo.bankingsystem.model.AccountType;
import com.wellsfargo.bankingsystem.model.Customer;
import com.wellsfargo.bankingsystem.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private CustomerService customerService;
    @Mock private AuditService auditService;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accountService = new AccountService(accountRepository, customerService, auditService);
    }

    @Test
    void openAccount_assignsSavingsInterestRate() {
        Customer customer = new Customer("Jane", "Doe", "jane@example.com", "NID-1", "555-0100", "123 Main St");
        customer.setId(1L);
        when(customerService.getById(1L)).thenReturn(customer);
        when(accountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountRequest request = new AccountRequest();
        request.setCustomerId(1L);
        request.setAccountType(AccountType.SAVINGS);
        request.setOpeningBalance(new BigDecimal("1000.00"));

        Account result = accountService.openAccount(request);

        assertEquals(new BigDecimal("0.0150"), result.getInterestRate());
        assertEquals(AccountType.SAVINGS, result.getAccountType());
        assertNotNull(result.getAccountNumber());
        assertTrue(result.getAccountNumber().startsWith("40"));
    }
}
