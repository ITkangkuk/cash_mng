package com.example.mng.controller;

import com.example.mng.entity.FinancialTransaction;
import com.example.mng.entity.TransactionType;
import com.example.mng.entity.User;
import com.example.mng.repository.FinancialTransactionRepository;
import com.example.mng.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class TransactionController {

    private final FinancialTransactionRepository financialTransactionRepository;
    private final UserRepository userRepository;

    public TransactionController(FinancialTransactionRepository financialTransactionRepository,
                                 UserRepository userRepository) {
        this.financialTransactionRepository = financialTransactionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/transactions")
    public String createTransaction(
            @RequestParam TransactionType type,
            @RequestParam String category,
            @RequestParam BigDecimal amount,
            @RequestParam LocalDate transactionDate,
            @RequestParam(defaultValue = "false") boolean paymentRequired,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String returnMonth,
            HttpSession session
    ) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        if (category.isBlank() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "redirect:/dashboard";
        }

        Long userId = (Long) loginUser;
        Object sessionUserName = session.getAttribute("userName");
        String createdByName = userRepository.findById(userId)
                .map(User::getUserName)
                .filter(userName -> userName != null && !userName.isBlank())
                .orElse(sessionUserName == null ? "알 수 없음" : String.valueOf(sessionUserName));

        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setUserId(userId);
        transaction.setCreatedByName(createdByName);
        transaction.setType(type);
        transaction.setCategory(category.trim());
        transaction.setAmount(amount);
        transaction.setTransactionDate(transactionDate);
        transaction.setPaymentRequired(paymentRequired);
        transaction.setPaymentCompleted(false);
        transaction.setDescription(description == null ? "" : description.trim());

        financialTransactionRepository.save(transaction);

        if (returnMonth == null || returnMonth.isBlank()) {
            return "redirect:/dashboard";
        }

        return "redirect:/dashboard?month=" + returnMonth;
    }

    @PostMapping("/transactions/{transactionId}/delete")
    public String deleteTransaction(
            @PathVariable Long transactionId,
            @RequestParam(required = false) String returnMonth,
            HttpSession session
    ) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        List<Long> sharedUserIds = resolveSharedUserIds((Long) loginUser);
        financialTransactionRepository.findById(transactionId)
                .filter(transaction -> sharedUserIds.contains(transaction.getUserId()))
                .ifPresent(financialTransactionRepository::delete);

        return redirectDashboard(returnMonth);
    }

    @PostMapping("/transactions/payment-complete")
    public String completePayments(
            @RequestParam(required = false) List<Long> transactionIds,
            @RequestParam(required = false) String returnMonth,
            HttpSession session
    ) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        if (transactionIds == null || transactionIds.isEmpty()) {
            return redirectDashboard(returnMonth);
        }

        List<Long> sharedUserIds = resolveSharedUserIds((Long) loginUser);
        List<FinancialTransaction> transactions = financialTransactionRepository.findAllById(transactionIds).stream()
                .filter(transaction -> sharedUserIds.contains(transaction.getUserId()))
                .filter(FinancialTransaction::isPaymentRequired)
                .toList();

        transactions.forEach(transaction -> {
            transaction.setPaymentRequired(false);
            transaction.setPaymentCompleted(true);
        });
        financialTransactionRepository.saveAll(transactions);

        return redirectDashboard(returnMonth);
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

    private String redirectDashboard(String returnMonth) {
        if (returnMonth == null || returnMonth.isBlank()) {
            return "redirect:/dashboard";
        }

        return "redirect:/dashboard?month=" + returnMonth;
    }
}
