package com.example.mng.repository;

import com.example.mng.entity.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {

    List<FinancialTransaction> findByUserIdInAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            List<Long> userIds,
            LocalDate startDate,
            LocalDate endDate
    );
}
