package com.wellsfargo.bankingsystem.repository;

import com.wellsfargo.bankingsystem.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountIdOrderByTimestampDesc(Long accountId);

    List<Transaction> findByAccountIdAndTimestampAfter(Long accountId, LocalDateTime after);

    List<Transaction> findByFlaggedTrueOrderByTimestampDesc();
}
