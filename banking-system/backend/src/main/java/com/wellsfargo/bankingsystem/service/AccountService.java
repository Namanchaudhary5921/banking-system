package com.wellsfargo.bankingsystem.service;

import com.wellsfargo.bankingsystem.dto.AccountRequest;
import com.wellsfargo.bankingsystem.exception.ResourceNotFoundException;
import com.wellsfargo.bankingsystem.model.Account;
import com.wellsfargo.bankingsystem.model.AccountType;
import com.wellsfargo.bankingsystem.model.Customer;
import com.wellsfargo.bankingsystem.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
public class AccountService {

    private static final BigDecimal SAVINGS_RATE = new BigDecimal("0.0150");  // 1.50% APY
    private static final BigDecimal CHECKING_RATE = new BigDecimal("0.0010"); // 0.10% APY

    private final AccountRepository accountRepository;
    private final CustomerService customerService;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    public AccountService(AccountRepository accountRepository, CustomerService customerService, AuditService auditService) {
        this.accountRepository = accountRepository;
        this.customerService = customerService;
        this.auditService = auditService;
    }

    @Transactional
    public Account openAccount(AccountRequest request) {
        Customer customer = customerService.getById(request.getCustomerId());

        BigDecimal rate = request.getAccountType() == AccountType.SAVINGS ? SAVINGS_RATE : CHECKING_RATE;

        Account account = new Account(
                generateAccountNumber(),
                customer,
                request.getAccountType(),
                request.getOpeningBalance() == null ? BigDecimal.ZERO : request.getOpeningBalance(),
                rate
        );

        Account saved = accountRepository.save(account);
        auditService.log("ACCOUNT", saved.getAccountNumber(), "OPENED",
                request.getAccountType() + " account opened for customer " + customer.getId(), "SYSTEM");
        return saved;
    }

    public Account getByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
    }

    public List<Account> getByCustomer(Long customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    public List<Account> getAll() {
        return accountRepository.findAll();
    }

    @Transactional
    public void closeAccount(String accountNumber) {
        Account account = getByAccountNumber(accountNumber);
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot close an account with a non-zero balance");
        }
        account.setActive(false);
        accountRepository.save(account);
        auditService.log("ACCOUNT", accountNumber, "CLOSED", "Account closed", "SYSTEM");
    }

    /**
     * Generates a pseudo-random 10 digit account number and guarantees
     * uniqueness against existing records before returning it.
     */
    private String generateAccountNumber() {
        String candidate;
        do {
            StringBuilder sb = new StringBuilder("40"); // bank routing prefix
            for (int i = 0; i < 8; i++) {
                sb.append(random.nextInt(10));
            }
            candidate = sb.toString();
        } while (accountRepository.findByAccountNumber(candidate).isPresent());
        return candidate;
    }
}
