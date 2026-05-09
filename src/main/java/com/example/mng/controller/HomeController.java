package com.example.mng.controller;

import com.example.mng.entity.FinancialTransaction;
import com.example.mng.entity.SpendingGoal;
import com.example.mng.entity.TransactionType;
import com.example.mng.entity.User;
import com.example.mng.repository.FinancialTransactionRepository;
import com.example.mng.repository.UserRepository;
import com.example.mng.service.SpendingGoalService;
import com.example.mng.service.TransactionOptionService;
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
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final FinancialTransactionRepository financialTransactionRepository;
    private final UserRepository userRepository;
    private final TransactionOptionService transactionOptionService;
    private final SpendingGoalService spendingGoalService;

    public HomeController(FinancialTransactionRepository financialTransactionRepository,
                          UserRepository userRepository,
                          TransactionOptionService transactionOptionService,
                          SpendingGoalService spendingGoalService) {
        this.financialTransactionRepository = financialTransactionRepository;
        this.userRepository = userRepository;
        this.transactionOptionService = transactionOptionService;
        this.spendingGoalService = spendingGoalService;
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
            @RequestParam(required = false) TransactionType filterType,
            @RequestParam(required = false) String filterCategory,
            @RequestParam(required = false) String filterSubcategory,
            @RequestParam(defaultValue = "dateDesc") String sort,
            HttpSession session,
            Model model
    ) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        YearMonth selectedMonth = parseMonth(month);
        Long userId = (Long) loginUser;
        List<FinancialTransaction> monthlyTransactions =
                findMonthlyTransactions(resolveSharedUserIds(userId), selectedMonth);
        List<FinancialTransaction> transactions = sortTransactions(
                filterTransactions(monthlyTransactions, filterType, filterCategory, filterSubcategory),
                sort
        );

        BigDecimal incomeTotal = sumByType(monthlyTransactions, TransactionType.INCOME);
        BigDecimal expenseTotal = sumByType(monthlyTransactions, TransactionType.EXPENSE);

        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("displayMonth", selectedMonth.getYear() + "년 " + selectedMonth.getMonthValue() + "월");
        model.addAttribute("prevMonth", selectedMonth.minusMonths(1));
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1));
        model.addAttribute("transactions", transactions);
        model.addAttribute("paymentRequiredTransactions", monthlyTransactions.stream()
                .filter(FinancialTransaction::isPaymentRequired)
                .toList());
        model.addAttribute("incomeTotal", incomeTotal);
        model.addAttribute("expenseTotal", expenseTotal);
        model.addAttribute("balance", incomeTotal.subtract(expenseTotal));
        model.addAttribute("categoryOptions", transactionOptionService.categoryOptions(userId));
        model.addAttribute("subcategoryOptions", transactionOptionService.subcategoryOptions(userId));
        model.addAttribute("typeOptions", transactionOptionService.typeOptions(userId));
        model.addAttribute("filterType", filterType);
        model.addAttribute("filterCategory", filterCategory);
        model.addAttribute("filterSubcategory", filterSubcategory);
        model.addAttribute("sort", sort);

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
        List<GoalProgress> goalProgressList = goalProgressList(userId, transactions);

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
        model.addAttribute("goalProgressList", goalProgressList);
        model.addAttribute("expenseGoalPercent", goalProgressList.stream()
                .filter(goal -> goal.subcategory() == null)
                .map(GoalProgress::percent)
                .max(Integer::compareTo)
                .orElse(0));
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
        model.addAttribute("categoryOptions", transactionOptionService.categoryOptions((Long) loginUser));
        model.addAttribute("subcategoryOptions", transactionOptionService.subcategoryOptions((Long) loginUser));
        model.addAttribute("typeOptions", transactionOptionService.typeOptions((Long) loginUser));

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

    private List<FinancialTransaction> filterTransactions(List<FinancialTransaction> transactions,
                                                          TransactionType filterType,
                                                          String filterCategory,
                                                          String filterSubcategory) {
        return transactions.stream()
                .filter(transaction -> filterType == null || transaction.getType() == filterType)
                .filter(transaction -> isBlank(filterCategory) || Objects.equals(transaction.getCategory(), filterCategory))
                .filter(transaction -> isBlank(filterSubcategory) || Objects.equals(transaction.getSubcategory(), filterSubcategory))
                .toList();
    }

    private List<FinancialTransaction> sortTransactions(List<FinancialTransaction> transactions, String sort) {
        Comparator<FinancialTransaction> comparator = switch (sort) {
            case "dateAsc" -> Comparator.comparing(FinancialTransaction::getTransactionDate)
                    .thenComparing(FinancialTransaction::getCreatedAt);
            case "amountDesc" -> Comparator.comparing(FinancialTransaction::getAmount).reversed()
                    .thenComparing(FinancialTransaction::getTransactionDate, Comparator.reverseOrder());
            case "amountAsc" -> Comparator.comparing(FinancialTransaction::getAmount)
                    .thenComparing(FinancialTransaction::getTransactionDate, Comparator.reverseOrder());
            case "categoryAsc" -> Comparator.comparing(FinancialTransaction::getCategory)
                    .thenComparing(FinancialTransaction::getSubcategory, Comparator.nullsLast(String::compareTo))
                    .thenComparing(FinancialTransaction::getTransactionDate, Comparator.reverseOrder());
            default -> Comparator.comparing(FinancialTransaction::getTransactionDate).reversed()
                    .thenComparing(FinancialTransaction::getCreatedAt, Comparator.reverseOrder());
        };

        return transactions.stream()
                .sorted(comparator)
                .toList();
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

    private List<GoalProgress> goalProgressList(Long userId, List<FinancialTransaction> transactions) {
        Map<String, BigDecimal> categoryExpenseMap = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        FinancialTransaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, FinancialTransaction::getAmount, BigDecimal::add)
                ));
        Map<String, BigDecimal> subcategoryExpenseMap = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .filter(transaction -> !isBlank(transaction.getSubcategory()))
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getCategory() + "\n" + transaction.getSubcategory(),
                        Collectors.reducing(BigDecimal.ZERO, FinancialTransaction::getAmount, BigDecimal::add)
                ));
        Map<Long, String> categoryLabelById = transactionOptionService.categoryOptions(userId).stream()
                .collect(Collectors.toMap(TransactionOptionService.CategoryOption::id, TransactionOptionService.CategoryOption::label));
        Map<Long, TransactionOptionService.SubcategoryOption> subcategoryById = transactionOptionService.subcategoryOptions(userId).stream()
                .collect(Collectors.toMap(TransactionOptionService.SubcategoryOption::id, option -> option));

        return spendingGoalService.goals(userId).stream()
                .map(goal -> toGoalProgress(goal, categoryLabelById, subcategoryById, categoryExpenseMap, subcategoryExpenseMap))
                .filter(Objects::nonNull)
                .toList();
    }

    private GoalProgress toGoalProgress(SpendingGoal goal,
                                        Map<Long, String> categoryLabelById,
                                        Map<Long, TransactionOptionService.SubcategoryOption> subcategoryById,
                                        Map<String, BigDecimal> categoryExpenseMap,
                                        Map<String, BigDecimal> subcategoryExpenseMap) {
        String category = categoryLabelById.get(goal.getCategoryOptionId());
        if (category == null) {
            return null;
        }

        String subcategory = null;
        BigDecimal usedAmount = categoryExpenseMap.getOrDefault(category, BigDecimal.ZERO);
        if (goal.getSubcategoryOptionId() != null) {
            TransactionOptionService.SubcategoryOption subcategoryOption = subcategoryById.get(goal.getSubcategoryOptionId());
            if (subcategoryOption == null) {
                return null;
            }
            subcategory = subcategoryOption.label();
            usedAmount = subcategoryExpenseMap.getOrDefault(category + "\n" + subcategory, BigDecimal.ZERO);
        }

        return new GoalProgress(
                category,
                subcategory,
                goal.getTargetAmount(),
                usedAmount,
                percent(usedAmount, goal.getTargetAmount())
        );
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record CategoryTotal(String category, BigDecimal amount, int percent) {
    }

    public record GoalProgress(String category, String subcategory, BigDecimal targetAmount, BigDecimal usedAmount, int percent) {
    }
}
