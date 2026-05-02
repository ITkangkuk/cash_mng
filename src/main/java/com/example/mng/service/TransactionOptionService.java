package com.example.mng.service;

import com.example.mng.entity.TransactionOptionKind;
import com.example.mng.entity.TransactionType;
import com.example.mng.entity.User;
import com.example.mng.repository.TransactionInputOptionRepository;
import com.example.mng.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionOptionService {

    private static final List<String> DEFAULT_CATEGORIES = List.of("식비", "월급", "교통비", "주거비", "통신비", "쇼핑");
    private static final List<TypeOption> DEFAULT_TYPES = List.of(
            new TypeOption(null, "지출", TransactionType.EXPENSE),
            new TypeOption(null, "수입", TransactionType.INCOME)
    );

    private final TransactionInputOptionRepository transactionInputOptionRepository;
    private final UserRepository userRepository;

    public TransactionOptionService(TransactionInputOptionRepository transactionInputOptionRepository,
                                    UserRepository userRepository) {
        this.transactionInputOptionRepository = transactionInputOptionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<CategoryOption> categoryOptions(Long userId) {
        String scopeKey = resolveScopeKey(userId);
        initializeDefaultsIfEmpty(scopeKey, TransactionOptionKind.CATEGORY);

        return transactionInputOptionRepository
                .findByScopeKeyAndOptionKindAndActiveTrueOrderByDisplayOrderAscOptionIdAsc(
                        scopeKey,
                        TransactionOptionKind.CATEGORY
                )
                .stream()
                .map(option -> new CategoryOption(option.getOptionId(), option.getLabel()))
                .toList();
    }

    @Transactional
    public List<TypeOption> typeOptions(Long userId) {
        String scopeKey = resolveScopeKey(userId);
        initializeDefaultsIfEmpty(scopeKey, TransactionOptionKind.TYPE);

        return transactionInputOptionRepository
                .findByScopeKeyAndOptionKindAndActiveTrueOrderByDisplayOrderAscOptionIdAsc(
                        scopeKey,
                        TransactionOptionKind.TYPE
                )
                .stream()
                .map(option -> new TypeOption(option.getOptionId(), option.getLabel(), option.getTypeValue()))
                .toList();
    }

    @Transactional
    public void addCategory(Long userId, String label) {
        addOption(userId, TransactionOptionKind.CATEGORY, label, null);
    }

    @Transactional
    public void addTypeOption(Long userId, String label, TransactionType typeValue) {
        addOption(userId, TransactionOptionKind.TYPE, label, typeValue);
    }

    @Transactional
    public void removeOption(Long userId, Long optionId) {
        String scopeKey = resolveScopeKey(userId);
        transactionInputOptionRepository.findByOptionIdAndScopeKey(optionId, scopeKey)
                .ifPresent(transactionInputOptionRepository::delete);
    }

    @Transactional
    public void resetCategories(Long userId) {
        String scopeKey = resolveScopeKey(userId);
        transactionInputOptionRepository.deleteByScopeKeyAndOptionKind(scopeKey, TransactionOptionKind.CATEGORY);
        saveDefaultCategories(scopeKey);
    }

    @Transactional
    public void resetTypeOptions(Long userId) {
        String scopeKey = resolveScopeKey(userId);
        transactionInputOptionRepository.deleteByScopeKeyAndOptionKind(scopeKey, TransactionOptionKind.TYPE);
        saveDefaultTypes(scopeKey);
    }

    private void addOption(Long userId, TransactionOptionKind optionKind, String label, TransactionType typeValue) {
        String normalizedLabel = normalizeLabel(label);
        if (normalizedLabel.isBlank()) {
            return;
        }

        String scopeKey = resolveScopeKey(userId);
        initializeDefaultsIfEmpty(scopeKey, optionKind);
        if (transactionInputOptionRepository.existsByScopeKeyAndOptionKindAndLabelIgnoreCase(
                scopeKey,
                optionKind,
                normalizedLabel
        )) {
            return;
        }

        insertOption(scopeKey, optionKind, normalizedLabel, typeValue, nextDisplayOrder(scopeKey, optionKind));
    }

    private void initializeDefaultsIfEmpty(String scopeKey, TransactionOptionKind optionKind) {
        if (transactionInputOptionRepository.countByScopeKeyAndOptionKind(scopeKey, optionKind) > 0) {
            return;
        }

        if (optionKind == TransactionOptionKind.CATEGORY) {
            saveDefaultCategories(scopeKey);
            return;
        }

        saveDefaultTypes(scopeKey);
    }

    private void saveDefaultCategories(String scopeKey) {
        for (int index = 0; index < DEFAULT_CATEGORIES.size(); index++) {
            insertOption(
                    scopeKey,
                    TransactionOptionKind.CATEGORY,
                    DEFAULT_CATEGORIES.get(index),
                    null,
                    index + 1
            );
        }
    }

    private void saveDefaultTypes(String scopeKey) {
        for (int index = 0; index < DEFAULT_TYPES.size(); index++) {
            TypeOption defaultType = DEFAULT_TYPES.get(index);
            insertOption(
                    scopeKey,
                    TransactionOptionKind.TYPE,
                    defaultType.label(),
                    defaultType.value(),
                    index + 1
            );
        }
    }

    private void insertOption(String scopeKey,
                              TransactionOptionKind optionKind,
                              String label,
                              TransactionType typeValue,
                              int displayOrder) {
        transactionInputOptionRepository.insertIgnoringDuplicate(
                scopeKey,
                optionKind.name(),
                label,
                typeValue == null ? null : typeValue.name(),
                displayOrder
        );
    }

    private int nextDisplayOrder(String scopeKey, TransactionOptionKind optionKind) {
        return transactionInputOptionRepository.countByScopeKeyAndOptionKind(scopeKey, optionKind) + 1;
    }

    private String resolveScopeKey(Long userId) {
        return userRepository.findById(userId)
                .map(User::getSharedGroupCode)
                .filter(sharedGroupCode -> sharedGroupCode != null && !sharedGroupCode.isBlank())
                .map(sharedGroupCode -> "GROUP:" + sharedGroupCode)
                .orElse("USER:" + userId);
    }

    private String normalizeLabel(String label) {
        if (label == null) {
            return "";
        }

        return label.trim().replaceAll("\\s+", " ");
    }

    public record CategoryOption(Long id, String label) {
    }

    public record TypeOption(Long id, String label, TransactionType value) {
    }
}
