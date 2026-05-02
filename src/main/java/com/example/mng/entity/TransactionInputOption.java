package com.example.mng.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_input_options")
public class TransactionInputOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long optionId;

    @Column(nullable = false, length = 100)
    private String scopeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionOptionKind optionKind;

    @Column(nullable = false, length = 50)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransactionType typeValue;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

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

    public Long getOptionId() {
        return optionId;
    }

    public String getScopeKey() {
        return scopeKey;
    }

    public void setScopeKey(String scopeKey) {
        this.scopeKey = scopeKey;
    }

    public TransactionOptionKind getOptionKind() {
        return optionKind;
    }

    public void setOptionKind(TransactionOptionKind optionKind) {
        this.optionKind = optionKind;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public TransactionType getTypeValue() {
        return typeValue;
    }

    public void setTypeValue(TransactionType typeValue) {
        this.typeValue = typeValue;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
