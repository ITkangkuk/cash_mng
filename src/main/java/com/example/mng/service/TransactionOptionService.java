package com.example.mng.service;

import com.example.mng.entity.TransactionOptionKind;
import com.example.mng.entity.TransactionSubcategoryOption;
import com.example.mng.entity.TransactionType;
import com.example.mng.entity.User;
import com.example.mng.repository.FinancialTransactionRepository;
import com.example.mng.repository.TransactionInputOptionRepository;
import com.example.mng.repository.TransactionSubcategoryOptionRepository;
import com.example.mng.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionOptionService {

    private static final List<String> DEFAULT_CATEGORIES = List.of("식비", "월급", "교통비", "주거비", "통신비", "쇼핑");
    private static final List<TypeOption> DEFAULT_TYPES = List.of(
            new TypeOption(null, "지출", TransactionType.EXPENSE),
            new TypeOption(null, "수입", TransactionType.INCOME)
    );

    private final FinancialTransactionRepository financialTransactionRepository;
    private final TransactionInputOptionRepository transactionInputOptionRepository;
    private final TransactionSubcategoryOptionRepository transactionSubcategoryOptionRepository;
    private final UserRepository userRepository;

    public TransactionOptionService(FinancialTransactionRepository financialTransactionRepository,
                                    TransactionInputOptionRepository transactionInputOptionRepository,
                                    TransactionSubcategoryOptionRepository transactionSubcategoryOptionRepository,
                                    UserRepository userRepository) {
        this.financialTransactionRepository = financialTransactionRepository;
        this.transactionInputOptionRepository = transactionInputOptionRepository;
        this.transactionSubcategoryOptionRepository = transactionSubcategoryOptionRepository;
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
    public void updateCategory(Long userId, Long optionId, String label) {
        updateOptionLabel(userId, optionId, TransactionOptionKind.CATEGORY, label);
    }

    @Transactional
    public void addTypeOption(Long userId, String label, TransactionType typeValue) {
        addOption(userId, TransactionOptionKind.TYPE, label, typeValue);
    }

    @Transactional
    public void updateTypeOption(Long userId, Long optionId, String label, TransactionType typeValue) {
        String normalizedLabel = normalizeLabel(label);
        if (normalizedLabel.isBlank()) {
            return;
        }

        String scopeKey = resolveScopeKey(userId);
        transactionInputOptionRepository.findByOptionIdAndScopeKey(optionId, scopeKey)
                .filter(option -> option.getOptionKind() == TransactionOptionKind.TYPE)
                .ifPresent(option -> {
                    option.setLabel(normalizedLabel);
                    option.setTypeValue(typeValue);
                    transactionInputOptionRepository.save(option);
                });
    }

    @Transactional
    public List<SubcategoryOption> subcategoryOptions(Long userId) {
        String scopeKey = resolveScopeKey(userId);
        Map<Long, String> categoryLabelById = transactionInputOptionRepository
                .findByScopeKeyAndOptionKindAndActiveTrueOrderByDisplayOrderAscOptionIdAsc(
                        scopeKey,
                        TransactionOptionKind.CATEGORY
                )
                .stream()
                .collect(Collectors.toMap(option -> option.getOptionId(), option -> option.getLabel()));

        return transactionSubcategoryOptionRepository
                .findByScopeKeyAndActiveTrueOrderByDisplayOrderAscSubcategoryIdAsc(scopeKey)
                .stream()
                .map(option -> new SubcategoryOption(
                        option.getSubcategoryId(),
                        option.getCategoryOptionId(),
                        categoryLabelById.getOrDefault(option.getCategoryOptionId(), ""),
                        option.getLabel()
                ))
                .toList();
    }

    @Transactional
    public void addSubcategory(Long userId, Long categoryOptionId, String label) {
        String normalizedLabel = normalizeLabel(label);
        if (normalizedLabel.isBlank()) {
            return;
        }

        String scopeKey = resolveScopeKey(userId);
        if (transactionInputOptionRepository.findByOptionIdAndScopeKey(categoryOptionId, scopeKey)
                .filter(option -> option.getOptionKind() == TransactionOptionKind.CATEGORY)
                .isEmpty()) {
            return;
        }

        if (transactionSubcategoryOptionRepository.existsByScopeKeyAndCategoryOptionIdAndLabelIgnoreCase(
                scopeKey,
                categoryOptionId,
                normalizedLabel
        )) {
            return;
        }

        TransactionSubcategoryOption option = new TransactionSubcategoryOption();
        option.setScopeKey(scopeKey);
        option.setCategoryOptionId(categoryOptionId);
        option.setLabel(normalizedLabel);
        option.setDisplayOrder(transactionSubcategoryOptionRepository.countByScopeKeyAndCategoryOptionId(scopeKey, categoryOptionId) + 1);
        transactionSubcategoryOptionRepository.save(option);
    }

    @Transactional
    public void updateSubcategory(Long userId, Long subcategoryId, String label) {
        String normalizedLabel = normalizeLabel(label);
        if (normalizedLabel.isBlank()) {
            return;
        }

        String scopeKey = resolveScopeKey(userId);
        transactionSubcategoryOptionRepository.findBySubcategoryIdAndScopeKey(subcategoryId, scopeKey)
                .ifPresent(option -> {
                    String oldLabel = option.getLabel();
                    String categoryLabel = transactionInputOptionRepository.findByOptionIdAndScopeKey(option.getCategoryOptionId(), scopeKey)
                            .map(category -> category.getLabel())
                            .orElse("");
                    option.setLabel(normalizedLabel);
                    transactionSubcategoryOptionRepository.save(option);
                    if (!categoryLabel.isBlank()) {
                        financialTransactionRepository.updateSubcategoryLabelForUsers(
                                resolveSharedUserIds(userId),
                                categoryLabel,
                                oldLabel,
                                normalizedLabel
                        );
                    }
                });
    }

    @Transactional
    public void removeSubcategory(Long userId, Long subcategoryId) {
        String scopeKey = resolveScopeKey(userId);
        transactionSubcategoryOptionRepository.findBySubcategoryIdAndScopeKey(subcategoryId, scopeKey)
                .ifPresent(transactionSubcategoryOptionRepository::delete);
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

    private void updateOptionLabel(Long userId, Long optionId, TransactionOptionKind optionKind, String label) {
        String normalizedLabel = normalizeLabel(label);
        if (normalizedLabel.isBlank()) {
            return;
        }

        String scopeKey = resolveScopeKey(userId);
        transactionInputOptionRepository.findByOptionIdAndScopeKey(optionId, scopeKey)
                .filter(option -> option.getOptionKind() == optionKind)
                .ifPresent(option -> {
                    String oldLabel = option.getLabel();
                    option.setLabel(normalizedLabel);
                    transactionInputOptionRepository.save(option);
                    if (optionKind == TransactionOptionKind.CATEGORY) {
                        financialTransactionRepository.updateCategoryLabelForUsers(
                                resolveSharedUserIds(userId),
                                oldLabel,
                                normalizedLabel
                        );
                    }
                });
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

    public String resolveScopeKey(Long userId) {
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

    private List<Long> resolveSharedUserIds(Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    String sharedGroupCode = user.getSharedGroupCode();
                    if (sharedGroupCode == null || sharedGroupCode.isBlank()) {
                        return List.of(userId);
                    }

                    List<Long> sharedUserIds = userRepository.findAllBySharedGroupCode(sharedGroupCode).stream()
                            .map(User::getUserId)
                            .toList();

                    return sharedUserIds.isEmpty() ? List.of(userId) : sharedUserIds;
                })
                .orElse(List.of(userId));
    }

    public record CategoryOption(Long id, String label) {
    }

    public record SubcategoryOption(Long id, Long categoryId, String categoryLabel, String label) {
    }

    public record TypeOption(Long id, String label, TransactionType value) {
    }
}
