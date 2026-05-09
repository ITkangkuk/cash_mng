package com.example.mng.service;

import com.example.mng.entity.SpendingGoal;
import com.example.mng.repository.SpendingGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpendingGoalService {

    private final SpendingGoalRepository spendingGoalRepository;
    private final TransactionOptionService transactionOptionService;

    public SpendingGoalService(SpendingGoalRepository spendingGoalRepository,
                               TransactionOptionService transactionOptionService) {
        this.spendingGoalRepository = spendingGoalRepository;
        this.transactionOptionService = transactionOptionService;
    }

    @Transactional(readOnly = true)
    public List<SpendingGoal> goals(Long userId) {
        return spendingGoalRepository.findByScopeKeyOrderByCategoryOptionIdAscSubcategoryOptionIdAsc(
                transactionOptionService.resolveScopeKey(userId)
        );
    }

    @Transactional(readOnly = true)
    public Map<String, SpendingGoal> goalMap(Long userId) {
        return goals(userId).stream()
                .collect(Collectors.toMap(
                        goal -> key(goal.getCategoryOptionId(), goal.getSubcategoryOptionId()),
                        goal -> goal,
                        (left, right) -> left
                ));
    }

    @Transactional
    public void saveGoal(Long userId, Long categoryOptionId, Long subcategoryOptionId, BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) < 0) {
            return;
        }

        String scopeKey = transactionOptionService.resolveScopeKey(userId);
        SpendingGoal goal = (subcategoryOptionId == null
                ? spendingGoalRepository.findByScopeKeyAndCategoryOptionIdAndSubcategoryOptionIdIsNull(scopeKey, categoryOptionId)
                : spendingGoalRepository.findByScopeKeyAndCategoryOptionIdAndSubcategoryOptionId(scopeKey, categoryOptionId, subcategoryOptionId))
                .orElseGet(SpendingGoal::new);

        goal.setScopeKey(scopeKey);
        goal.setCategoryOptionId(categoryOptionId);
        goal.setSubcategoryOptionId(subcategoryOptionId);
        goal.setTargetAmount(targetAmount);
        spendingGoalRepository.save(goal);
    }

    public String key(Long categoryOptionId, Long subcategoryOptionId) {
        return categoryOptionId + ":" + (subcategoryOptionId == null ? "" : subcategoryOptionId);
    }
}
