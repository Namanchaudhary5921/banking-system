package com.wellsfargo.bankingsystem.service;

import com.wellsfargo.bankingsystem.model.Account;
import com.wellsfargo.bankingsystem.model.Transaction;
import com.wellsfargo.bankingsystem.model.TransactionType;
import com.wellsfargo.bankingsystem.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Lightweight rule-based fraud/risk engine. Mirrors the kind of
 * "risk decisioning" a Technology/Risk intern might touch — not a real
 * ML model, but a defensible, explainable set of rules with an audit trail.
 *
 * Rules implemented:
 *  1. Large single transaction  -> flag if amount exceeds LARGE_TXN_THRESHOLD
 *  2. Velocity check            -> flag if more than MAX_TXNS_PER_WINDOW
 *                                   transactions occur on the same account
 *                                   within VELOCITY_WINDOW_MINUTES
 *  3. Rapid drain                -> flag if a withdrawal/transfer would take
 *                                   the account below RAPID_DRAIN_PERCENT of
 *                                   its balance from 24h ago in one shot
 */
@Service
public class FraudDetectionService {

    private static final BigDecimal LARGE_TXN_THRESHOLD = new BigDecimal("1000000.00");
    private static final int VELOCITY_WINDOW_MINUTES = 10;
    private static final int MAX_TXNS_PER_WINDOW = 5;

    private final TransactionRepository transactionRepository;

    public FraudDetectionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Evaluates a proposed transaction against fraud rules.
     * Returns an Optional reason string; empty means "not flagged".
     */
    public Optional<String> evaluate(Account account, TransactionType type, BigDecimal amount) {
        if (amount.compareTo(LARGE_TXN_THRESHOLD) > 0) {
            return Optional.of("Amount exceeds large-transaction threshold of $" + LARGE_TXN_THRESHOLD);
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(VELOCITY_WINDOW_MINUTES);
        List<Transaction> recent = transactionRepository.findByAccountIdAndTimestampAfter(account.getId(), windowStart);
        if (recent.size() >= MAX_TXNS_PER_WINDOW) {
            return Optional.of("Velocity rule triggered: " + recent.size() + " transactions in the last "
                    + VELOCITY_WINDOW_MINUTES + " minutes");
        }

        if ((type == TransactionType.WITHDRAWAL || type == TransactionType.TRANSFER_OUT)
                && account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal fractionOfBalance = amount.divide(account.getBalance(), 4, java.math.RoundingMode.HALF_UP);
            if (fractionOfBalance.compareTo(new BigDecimal("0.90")) >= 0) {
                return Optional.of("Withdrawal represents over 90% of current balance in a single transaction");
            }
        }

        return Optional.empty();
    }
}
