package com.example.mng.controller;

import com.example.mng.entity.User;
import com.example.mng.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkLogin(
            @CookieValue(value = "AUTO_LOGIN_TOKEN", required = false) String autoLoginToken,
            HttpSession session
    ) {
        if (session.getAttribute("loginUser") != null) {
            return ResponseEntity.ok(Map.of("loggedIn", true));
        }

        if (autoLoginToken == null || autoLoginToken.isBlank()) {
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }

        Optional<User> optionalUser = userRepository.findByAutoLoginToken(autoLoginToken);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }

        User user = optionalUser.get();

        if (user.getAutoLoginExpireAt() == null ||
            user.getAutoLoginExpireAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }

        session.setAttribute("loginUser", user.getUserId());
        session.setAttribute("loginId", user.getLoginId());
        session.setAttribute("userName", user.getUserName());

        return ResponseEntity.ok(Map.of("loggedIn", true));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam String loginId,
            @RequestParam String password,
            @RequestParam(defaultValue = "false") boolean rememberMe,
            HttpSession session,
            HttpServletResponse response
    ) {
        Optional<User> optionalUser = userRepository.findByLoginId(loginId);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "아이디 또는 비밀번호가 올바르지 않습니다."
            ));
        }

        User user = optionalUser.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "아이디 또는 비밀번호가 올바르지 않습니다."
            ));
        }

        session.setAttribute("loginUser", user.getUserId());
        session.setAttribute("loginId", user.getLoginId());
        session.setAttribute("userName", user.getUserName());

        if (rememberMe) {
            String token = UUID.randomUUID().toString();
            LocalDateTime expireAt = LocalDateTime.now().plusDays(30);

            user.setAutoLoginToken(token);
            user.setAutoLoginExpireAt(expireAt);
            userRepository.save(user);

            Cookie cookie = new Cookie("AUTO_LOGIN_TOKEN", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24 * 30);

            response.addCookie(cookie);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "로그인 성공"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Boolean>> logout(
            HttpSession session,
            HttpServletResponse response
    ) {
        session.invalidate();

        Cookie cookie = new Cookie("AUTO_LOGIN_TOKEN", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("success", true));
    }
}