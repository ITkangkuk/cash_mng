package com.example.mng.controller;

import com.example.mng.service.SpendingGoalService;
import com.example.mng.service.TransactionOptionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class GoalController {

    private final TransactionOptionService transactionOptionService;
    private final SpendingGoalService spendingGoalService;

    public GoalController(TransactionOptionService transactionOptionService,
                          SpendingGoalService spendingGoalService) {
        this.transactionOptionService = transactionOptionService;
        this.spendingGoalService = spendingGoalService;
    }

    @GetMapping("/goals")
    public String goals(HttpSession session, Model model) {
        Long userId = loginUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("categoryOptions", transactionOptionService.categoryOptions(userId));
        model.addAttribute("subcategoryOptions", transactionOptionService.subcategoryOptions(userId));
        model.addAttribute("goalMap", spendingGoalService.goalMap(userId));

        return "goals";
    }

    @PostMapping("/goals")
    public String saveGoal(
            @RequestParam Long categoryOptionId,
            @RequestParam(required = false) Long subcategoryOptionId,
            @RequestParam BigDecimal targetAmount,
            HttpSession session
    ) {
        Long userId = loginUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        spendingGoalService.saveGoal(userId, categoryOptionId, subcategoryOptionId, targetAmount);
        return "redirect:/goals";
    }

    private Long loginUserId(HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        return loginUser instanceof Long userId ? userId : null;
    }
}
