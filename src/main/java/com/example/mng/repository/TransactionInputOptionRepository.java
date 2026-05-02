package com.example.mng.repository;

import com.example.mng.entity.TransactionInputOption;
import com.example.mng.entity.TransactionOptionKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionInputOptionRepository extends JpaRepository<TransactionInputOption, Long> {

    List<TransactionInputOption> findByScopeKeyAndOptionKindAndActiveTrueOrderByDisplayOrderAscOptionIdAsc(
            String scopeKey,
            TransactionOptionKind optionKind
    );

    Optional<TransactionInputOption> findByOptionIdAndScopeKey(Long optionId, String scopeKey);

    boolean existsByScopeKeyAndOptionKindAndLabelIgnoreCase(String scopeKey, TransactionOptionKind optionKind, String label);

    int countByScopeKeyAndOptionKind(String scopeKey, TransactionOptionKind optionKind);

    void deleteByScopeKeyAndOptionKind(String scopeKey, TransactionOptionKind optionKind);

    @Modifying
    @Query(value = """
            INSERT INTO transaction_input_options (
                scope_key,
                option_kind,
                label,
                type_value,
                display_order,
                active,
                created_at,
                updated_at
            )
            VALUES (
                :scopeKey,
                :optionKind,
                :label,
                :typeValue,
                :displayOrder,
                TRUE,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIgnoringDuplicate(
            @Param("scopeKey") String scopeKey,
            @Param("optionKind") String optionKind,
            @Param("label") String label,
            @Param("typeValue") String typeValue,
            @Param("displayOrder") int displayOrder
    );
}
