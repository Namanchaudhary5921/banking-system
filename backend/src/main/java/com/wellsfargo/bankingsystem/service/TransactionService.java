package com.wellsfargo.bankingsystem.service;

import com.wellsfargo.bankingsystem.exception.InsufficientFundsException;
import com.wellsfargo.bankingsystem.exception.ResourceNotFoundException;
import com.wellsfargo.bankingsystem.model.Account;
import com.wellsfargo.bankingsystem.model.Transaction;
import com.wellsfargo.bankingsystem.model.TransactionType;
import com.wellsfargo.bankingsystem.repository.AccountRepository;
import com.wellsfargo.bankingsystem.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Core money-movement logic. Every method that touches balances is
 * @Transactional so a failure partway through (e.g. insufficient funds on
 * the second leg of a transfer) rolls back everything already written in
 * that method - no half-completed transfers.
 *
 * Row-level locking (see AccountRepository.findByAccountNumberForUpdate)
 * additionally prevents two concurrent requests from reading the same
 * stale balance and both succeeding when only one should.
 */
@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FraudDetectionService fraudDetectionService;
    private final AuditService auditService;

    public TransactionService(AccountRepository accountRepository,
                               TransactionRepository transactionRepository,
                               FraudDetectionService fraudDetectionService,
                               AuditService auditService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.fraudDetectionService = fraudDetectionService;
        this.auditService = auditService;
    }

    @Transactional
    public Transaction deposit(String accountNumber, BigDecimal amount) {
        Account account = lockAccount(accountNumber);

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction txn = recordTransaction(account, TransactionType.DEPOSIT, amount, newBalance, null);
        auditService.log("TRANSACTION", txn.getId().toString(), "DEPOSIT",
                "Deposit of $" + amount + " to " + accountNumber, "SYSTEM");
        return txn;
    }

    @Transactional
    public Transaction withdraw(String accountNumber, BigDecimal amount) {
        Account account = lockAccount(accountNumber);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account " + accountNumber);
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction txn = recordTransaction(account, TransactionType.WITHDRAWAL, amount, newBalance, null);
        auditService.log("TRANSACTION", txn.getId().toString(), "WITHDRAWAL",
                "Withdrawal of $" + amount + " from " + accountNumber, "SYSTEM");
        return txn;
    }

    /**
     * Transfers funds between two accounts as a single atomic operation.
     * Accounts are locked in a consistent order (by account number) to
     * avoid deadlocks when two transfers happen in opposite directions
     * at the same time.
     */
    @Transactional
    public void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        String first = fromAccountNumber.compareTo(toAccountNumber) < 0 ? fromAccountNumber : toAccountNumber;
        String second = fromAccountNumber.compareTo(toAccountNumber) < 0 ? toAccountNumber : fromAccountNumber;

        Account lockedFirst = lockAccount(first);
        Account lockedSecond = lockAccount(second);

        Account fromAccount = fromAccountNumber.equals(first) ? lockedFirst : lockedSecond;
        Account toAccount = toAccountNumber.equals(first) ? lockedFirst : lockedSecond;

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account " + fromAccountNumber);
        }

        BigDecimal fromNewBalance = fromAccount.getBalance().subtract(amount);
        BigDecimal toNewBalance = toAccount.getBalance().add(amount);

        fromAccount.setBalance(fromNewBalance);
        toAccount.setBalance(toNewBalance);
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        recordTransaction(fromAccount, TransactionType.TRANSFER_OUT, amount, fromNewBalance, toAccountNumber);
        recordTransaction(toAccount, TransactionType.TRANSFER_IN, amount, toNewBalance, fromAccountNumber);

        auditService.log("TRANSACTION", fromAccountNumber, "TRANSFER",
                "Transferred $" + amount + " from " + fromAccountNumber + " to " + toAccountNumber, "SYSTEM");
        // Any exception above (e.g. insufficient funds) triggers a full
        // rollback of both legs, so the transfer never partially completes.
    }

    public List<Transaction> getHistory(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
        return transactionRepository.findByAccountIdOrderByTimestampDesc(account.getId());
    }

    public List<Transaction> getFlaggedTransactions() {
        return transactionRepository.findByFlaggedTrueOrderByTimestampDesc();
    }

    private Account lockAccount(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
    }

    private Transaction recordTransaction(Account account, TransactionType type, BigDecimal amount,
                                           BigDecimal balanceAfter, String relatedAccountNumber) {
        Transaction txn = new Transaction(account, type, amount, balanceAfter, relatedAccountNumber);

        Optional<String> flagReason = fraudDetectionService.evaluate(account, type, amount);
        if (flagReason.isPresent()) {
            txn.setFlagged(true);
            txn.setFlagReason(flagReason.get());
        }

        Transaction saved = transactionRepository.save(txn);

        if (saved.isFlagged()) {
            auditService.log("TRANSACTION", saved.getId().toString(), "FLAGGED", saved.getFlagReason(), "FRAUD_ENGINE");
        }

        return saved;
    }
}
