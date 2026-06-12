```java
package com.sangam.controller;

import com.sangam.dto.LoginRequest;
import com.sangam.dto.OtpRequest;
import com.sangam.dto.RegisterRequest;
import com.sangam.service.AuthService;
import com.sangam.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log =
            LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final EmailService emailService;

    public AuthController(
            AuthService authService,
            EmailService emailService) {

        this.authService = authService;
        this.emailService = emailService;
    }

    // ============================================================
    // REGISTRATION OTP
    // ============================================================

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(
            @RequestBody Map<String, String> body) {

        try {

            String email = body.get("email");

            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Email is required."
                        ));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            authService.sendOtp(
                                    email.trim().toLowerCase()
                            )
                    )
            );

        } catch (Exception e) {

            log.error("Send OTP failed", e);

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Unable to send OTP."
                    ));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody OtpRequest request) {

        try {

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            authService.verifyOtp(
                                    request.getEmail(),
                                    request.getOtp()
                            )
                    )
            );

        } catch (Exception e) {

            log.error("OTP verification failed", e);

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "OTP verification failed."
                    ));
        }
    }

    // ============================================================
    // REGISTER
    // ============================================================

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {

        try {

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            authService.register(request)
                    )
            );

        } catch (Exception e) {

            log.error("Registration failed", e);

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Registration failed."
                    ));
        }
    }

    // ============================================================
    // MEMBER LOGIN
    // ============================================================

    @PostMapping("/member-login")
    public ResponseEntity<?> memberLogin(
            @RequestBody LoginRequest request) {

        try {

            return ResponseEntity.ok(
                    authService.memberLogin(
                            request.getPhoneNumber(),
                            request.getPassword()
                    )
            );

        } catch (Exception e) {

            log.error("Member login failed", e);

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Invalid credentials."
                    ));
        }
    }

    // ============================================================
    // ADMIN LOGIN
    // ============================================================

    @PostMapping("/admin-login")
    public ResponseEntity<?> adminLogin(
            @RequestBody LoginRequest request) {

        try {

            return ResponseEntity.ok(
                    authService.adminLogin(
                            request.getPhoneNumber(),
                            request.getPassword()
                    )
            );

        } catch (Exception e) {

            log.error("Admin login failed", e);

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Invalid admin credentials."
                    ));
        }
    }

    // ============================================================
    // CONTACT FORM
    // ============================================================

    @PostMapping("/contact")
    public ResponseEntity<?> contactAdmin(
            @RequestBody Map<String, String> body) {

        try {

            String name =
                    body.getOrDefault("name", "Unknown");

            String email =
                    body.getOrDefault("email", "");

            String phone =
                    body.getOrDefault("phone", "");

            String message =
                    body.getOrDefault("message", "");

            if (message.isBlank()) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Message cannot be empty."
                        ));
            }

            emailService.sendContactMessageToAdmin(
                    name,
                    email,
                    phone,
                    message
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Message sent successfully."
                    )
            );

        } catch (Exception e) {

            log.error("Contact form failed", e);

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Unable to send message."
                    ));
        }
    }

    // ============================================================
    // FORGOT PASSWORD
    // ============================================================

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> forgotPasswordSendOtp(
            @RequestBody Map<String, String> body) {

        try {

            String email = body.get("email");

            if (email == null || email.isBlank()) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Email is required."
                        ));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            authService.forgotPasswordSendOtp(
                                    email.trim().toLowerCase()
                            )
                    )
            );

        } catch (Exception e) {

            log.error("Forgot password OTP failed", e);

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Unable to process request."
                    ));
        }
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> forgotPasswordVerifyOtp(
            @RequestBody Map<String, String> body) {

        try {

            String email = body.get("email");
            String otp = body.get("otp");

            if (email == null || otp == null) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Email and OTP are required."
                        ));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            authService.forgotPasswordVerifyOtp(
                                    email.trim().toLowerCase(),
                                    otp.trim()
                            )
                    )
            );

        } catch (Exception e) {

            log.error("Forgot password OTP verification failed", e);

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "OTP verification failed."
                    ));
        }
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(
            @RequestBody Map<String, String> body) {

        try {

            String email =
                    body.get("email");

            String newPassword =
                    body.get("newPassword");

            if (email == null ||
                    newPassword == null ||
                    newPassword.isBlank()) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Email and password are required."
                        ));
            }

            if (newPassword.length() < 8) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Password must be at least 8 characters."
                        ));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            authService.resetPassword(
                                    email.trim().toLowerCase(),
                                    newPassword
                            )
                    )
            );

        } catch (Exception e) {

            log.error("Password reset failed", e);

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Password reset failed."
                    ));
        }
    }
}
```
