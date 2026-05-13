package com.sangam.controller;

import com.sangam.dto.LoginRequest;
import com.sangam.dto.LoginResponse;
import com.sangam.dto.OtpRequest;
import com.sangam.dto.RegisterRequest;
import com.sangam.service.AuthService;
import com.sangam.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
// ── FIX (Bug 10): Removed @CrossOrigin — rely solely on SecurityConfig CORS ──
public class AuthController {

    private final AuthService  authService;
    private final EmailService emailService;

    public AuthController(AuthService authService, EmailService emailService) {
        this.authService  = authService;
        this.emailService = emailService;
    }

    // ── Send OTP ──────────────────────────────────────────────────
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        try {
            // ── FIX (Bug 2): Validate email before passing to service ──
            String email = body.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required."));
            }
            return ResponseEntity.ok(
                Map.of("message", authService.sendOtp(email.trim().toLowerCase()))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Verify OTP ────────────────────────────────────────────────
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpRequest req) {
        try {
            return ResponseEntity.ok(
                Map.of("message", authService.verifyOtp(req.getEmail(), req.getOtp()))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Register ──────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            return ResponseEntity.ok(
                Map.of("message", authService.register(req))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Member Login ──────────────────────────────────────────────
    @PostMapping("/member-login")
    public ResponseEntity<?> memberLogin(@RequestBody LoginRequest req) {
        try {
            LoginResponse res = authService.memberLogin(req.getPhoneNumber(), req.getPassword());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Admin Login ───────────────────────────────────────────────
    @PostMapping("/admin-login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest req) {
        try {
            LoginResponse res = authService.adminLogin(req.getPhoneNumber(), req.getPassword());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Contact Form ──────────────────────────────────────────────
    @PostMapping("/contact")
    public ResponseEntity<?> contactAdmin(@RequestBody Map<String, String> body) {
        try {
            String name    = body.getOrDefault("name",    "Unknown");
            String email   = body.getOrDefault("email",   "");
            String phone   = body.getOrDefault("phone",   "");
            String message = body.getOrDefault("message", "");

            if (message.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message cannot be empty."));
            }

            emailService.sendContactMessageToAdmin(name, email, phone, message);
            return ResponseEntity.ok(Map.of("message", "Message sent to admin successfully!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to send message: " + e.getMessage()));
        }
    }
}
