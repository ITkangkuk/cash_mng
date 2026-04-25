package com.example.mng.controller;

import com.example.mng.entity.FinancialTransaction;
import com.example.mng.entity.TransactionType;
import com.example.mng.entity.User;
import com.example.mng.repository.FinancialTransactionRepository;
import com.example.mng.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final FinancialTransactionRepository financialTransactionRepository;
    private final UserRepository userRepository;

    public HomeController(FinancialTransactionRepository financialTransactionRepository,
                          UserRepository userRepository) {
        this.financialTransactionRepository = financialTransactionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) String month,
            HttpSession session,
            Model model
    ) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        YearMonth selectedMonth = parseMonth(month);
        Long userId = (Long) loginUser;
        List<FinancialTransaction> transactions =
                findMonthlyTransactions(resolveSharedUserIds(userId), selectedMonth);

        BigDecimal incomeTotal = sumByType(transactions, TransactionType.INCOME);
        BigDecimal expenseTotal = sumByType(transactions, TransactionType.EXPENSE);

        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("displayMonth", selectedMonth.getYear() + "년 " + selectedMonth.getMonthValue() + "월");
        model.addAttribute("prevMonth", selectedMonth.minusMonths(1));
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1));
        model.addAttribute("transactions", transactions);
        model.addAttribute("paymentRequiredTransactions", transactions.stream()
                .filter(FinancialTransaction::isPaymentRequired)
                .toList());
        model.addAttribute("incomeTotal", incomeTotal);
        model.addAttribute("expenseTotal", expenseTotal);
        model.addAttribute("balance", incomeTotal.subtract(expenseTotal));

        return "dashboard";
    }

    @GetMapping("/report")
    public String report(
            @RequestParam(required = false) String month,
            HttpSession session,
            Model model
    ) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        YearMonth selectedMonth = parseMonth(month);
        YearMonth previousMonth = selectedMonth.minusMonths(1);
        Long userId = (Long) loginUser;

        List<Long> sharedUserIds = resolveSharedUserIds(userId);
        List<FinancialTransaction> transactions = findMonthlyTransactions(sharedUserIds, selectedMonth);
        List<FinancialTransaction> previousTransactions = findMonthlyTransactions(sharedUserIds, previousMonth);

        BigDecimal incomeTotal = sumByType(transactions, TransactionType.INCOME);
        BigDecimal expenseTotal = sumByType(transactions, TransactionType.EXPENSE);
        BigDecimal previousIncomeTotal = sumByType(previousTransactions, TransactionType.INCOME);
        BigDecimal previousExpenseTotal = sumByType(previousTransactions, TransactionType.EXPENSE);

        List<CategoryTotal> expenseCategoryTotals = categoryTotals(transactions, TransactionType.EXPENSE);

        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("displayMonth", selectedMonth.getYear() + "년 " + selectedMonth.getMonthValue() + "월");
        model.addAttribute("prevMonth", selectedMonth.minusMonths(1));
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1));
        model.addAttribute("previousDisplayMonth", previousMonth.getYear() + "년 " + previousMonth.getMonthValue() + "월");
        model.addAttribute("incomeTotal", incomeTotal);
        model.addAttribute("expenseTotal", expenseTotal);
        model.addAttribute("balance", incomeTotal.subtract(expenseTotal));
        model.addAttribute("previousIncomeTotal", previousIncomeTotal);
        model.addAttribute("previousExpenseTotal", previousExpenseTotal);
        model.addAttribute("incomeDifference", incomeTotal.subtract(previousIncomeTotal));
        model.addAttribute("expenseDifference", expenseTotal.subtract(previousExpenseTotal));
        model.addAttribute("expenseCategoryTotals", expenseCategoryTotals);
        model.addAttribute("expenseGoalPercent", percent(expenseTotal, new BigDecimal("1000000")));
        model.addAttribute("savingRatePercent", percent(incomeTotal.subtract(expenseTotal), incomeTotal));

        return "report";
    }

    @GetMapping("/settings")
    public String settings(HttpSession session, Model model) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("userName", session.getAttribute("userName"));

        return "settings";
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

    private List<FinancialTransaction> findMonthlyTransactions(List<Long> userIds, YearMonth selectedMonth) {
        return financialTransactionRepository.findByUserIdInAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                userIds,
                selectedMonth.atDay(1),
                selectedMonth.atEndOfMonth()
        );
    }

    private BigDecimal sumByType(List<FinancialTransaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CategoryTotal> categoryTotals(List<FinancialTransaction> transactions, TransactionType type) {
        Map<String, BigDecimal> amountByCategory = transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .collect(Collectors.groupingBy(
                        FinancialTransaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, FinancialTransaction::getAmount, BigDecimal::add)
                ));

        BigDecimal maxAmount = amountByCategory.values().stream()
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        return amountByCategory.entrySet().stream()
                .map(entry -> new CategoryTotal(entry.getKey(), entry.getValue(), percent(entry.getValue(), maxAmount)))
                .sorted(Comparator.comparing(CategoryTotal::amount).reversed())
                .toList();
    }

    private int percent(BigDecimal value, BigDecimal maxValue) {
        if (maxValue == null || maxValue.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        return value.multiply(new BigDecimal("100"))
                .divide(maxValue, 0, java.math.RoundingMode.HALF_UP)
                .min(new BigDecimal("100"))
                .max(BigDecimal.ZERO)
                .intValue();
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }

        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            return YearMonth.now();
        }
    }

    public record CategoryTotal(String category, BigDecimal amount, int percent) {
    }
}
