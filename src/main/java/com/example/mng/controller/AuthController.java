package com.example.mng.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkLogin(HttpSession session) {
        boolean loggedIn = session.getAttribute("loginUser") != null;
        return ResponseEntity.ok(Map.of("loggedIn", loggedIn));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam String loginId,
            @RequestParam String password,
            HttpSession session
    ) {
        if ("admin".equals(loginId) && "1234".equals(password)) {
            session.setAttribute("loginUser", loginId);
            return ResponseEntity.ok(Map.of("success", true));
        }

        return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "아이디 또는 비밀번호가 올바르지 않습니다."
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Boolean>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true));
    }
}