package com.example.mng.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "spending_goals")
public class SpendingGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long goalId;

    @Column(nullable = false, length = 100)
    private String scopeKey;

    @Column(nullable = false)
    private Long categoryOptionId;

    private Long subcategoryOptionId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getGoalId() {
        return goalId;
    }

    public String getScopeKey() {
        return scopeKey;
    }

    public void setScopeKey(String scopeKey) {
        this.scopeKey = scopeKey;
    }

    public Long getCategoryOptionId() {
        return categoryOptionId;
    }

    public void setCategoryOptionId(Long categoryOptionId) {
        this.categoryOptionId = categoryOptionId;
    }

    public Long getSubcategoryOptionId() {
        return subcategoryOptionId;
    }

    public void setSubcategoryOptionId(Long subcategoryOptionId) {
        this.subcategoryOptionId = subcategoryOptionId;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }
}
