package com.example.mng.repository;

import com.example.mng.entity.TransactionSubcategoryOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionSubcategoryOptionRepository extends JpaRepository<TransactionSubcategoryOption, Long> {

    List<TransactionSubcategoryOption> findByScopeKeyAndActiveTrueOrderByDisplayOrderAscSubcategoryIdAsc(String scopeKey);

    List<TransactionSubcategoryOption> findByScopeKeyAndCategoryOptionIdAndActiveTrueOrderByDisplayOrderAscSubcategoryIdAsc(
            String scopeKey,
            Long categoryOptionId
    );

    Optional<TransactionSubcategoryOption> findBySubcategoryIdAndScopeKey(Long subcategoryId, String scopeKey);

    boolean existsByScopeKeyAndCategoryOptionIdAndLabelIgnoreCase(String scopeKey, Long categoryOptionId, String label);

    int countByScopeKeyAndCategoryOptionId(String scopeKey, Long categoryOptionId);
}
