package com.example.mng.controller;

import com.example.mng.entity.TransactionType;
import com.example.mng.service.TransactionOptionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SettingsController {

    private final TransactionOptionService transactionOptionService;

    public SettingsController(TransactionOptionService transactionOptionService) {
        this.transactionOptionService = transactionOptionService;
    }

    @PostMapping("/settings/categories")
    public String addCategory(@RequestParam String label, HttpSession session) {
        Long userId = loginUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        transactionOptionService.addCategory(userId, label);
        return "redirect:/settings";
    }

    @PostMapping("/settings/categories/reset")
    public String resetCategories(HttpSession session) {
        Long userId = loginUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        transactionOptionService.resetCategories(userId);
        return "redirect:/settings";
    }

    @PostMapping("/settings/types")
    public String addTypeOption(
            @RequestParam String label,
            @RequestParam TransactionType typeValue,
            HttpSession session
    ) {
        Long userId = loginUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        transactionOptionService.addTypeOption(userId, label, typeValue);
        return "redirect:/settings";
    }

    @PostMapping("/settings/types/reset")
    public String resetTypeOptions(HttpSession session) {
        Long userId = loginUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        transactionOptionService.resetTypeOptions(userId);
        return "redirect:/settings";
    }

    @PostMapping("/settings/options/{optionId}/delete")
    public String removeOption(@PathVariable Long optionId, HttpSession session) {
        Long userId = loginUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        transactionOptionService.removeOption(userId, optionId);
        return "redirect:/settings";
    }

    private Long loginUserId(HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        return loginUser instanceof Long userId ? userId : null;
    }
}
