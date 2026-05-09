package com.example.mng.repository;

import com.example.mng.entity.SpendingGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpendingGoalRepository extends JpaRepository<SpendingGoal, Long> {

    List<SpendingGoal> findByScopeKeyOrderByCategoryOptionIdAscSubcategoryOptionIdAsc(String scopeKey);

    Optional<SpendingGoal> findByScopeKeyAndCategoryOptionIdAndSubcategoryOptionId(
            String scopeKey,
            Long categoryOptionId,
            Long subcategoryOptionId
    );

    Optional<SpendingGoal> findByScopeKeyAndCategoryOptionIdAndSubcategoryOptionIdIsNull(
            String scopeKey,
            Long categoryOptionId
    );
}
