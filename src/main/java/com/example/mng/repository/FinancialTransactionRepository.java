package com.example.mng.repository;

import com.example.mng.entity.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {

    List<FinancialTransaction> findByUserIdInAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            List<Long> userIds,
            LocalDate startDate,
            LocalDate endDate
    );

    @Modifying
    @Query("""
            UPDATE FinancialTransaction transaction
            SET transaction.category = :newLabel
            WHERE transaction.userId IN :userIds
              AND transaction.category = :oldLabel
            """)
    int updateCategoryLabelForUsers(
            @Param("userIds") List<Long> userIds,
            @Param("oldLabel") String oldLabel,
            @Param("newLabel") String newLabel
    );

    @Modifying
    @Query("""
            UPDATE FinancialTransaction transaction
            SET transaction.subcategory = :newLabel
            WHERE transaction.userId IN :userIds
              AND transaction.category = :categoryLabel
              AND transaction.subcategory = :oldLabel
            """)
    int updateSubcategoryLabelForUsers(
            @Param("userIds") List<Long> userIds,
            @Param("categoryLabel") String categoryLabel,
            @Param("oldLabel") String oldLabel,
            @Param("newLabel") String newLabel
    );
}
