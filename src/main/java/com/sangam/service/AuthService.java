package com.sangam.service;

import com.sangam.dto.LoginResponse;
import com.sangam.dto.RegisterRequest;
import com.sangam.entity.Member;
import com.sangam.entity.OtpStore;
import com.sangam.repository.MemberRepository;
import com.sangam.repository.OtpStoreRepository;
import com.sangam.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final MemberRepository   memberRepository;
    private final OtpStoreRepository otpStoreRepository;
    private final PasswordEncoder    passwordEncoder;
    private final EmailService       emailService;
    private final JwtUtil            jwtUtil;

    @Value("${app.admin.phone}")
    private String adminPhone;

   @Value("${app.admin.password}")
private String adminPasswordHash;

    private static final SecureRandom SECURE_RANDOM       = new SecureRandom();
    private static final int          OTP_COOLDOWN_SECONDS = 60;

    private final Map<String, LocalDateTime> otpCooldown = new ConcurrentHashMap<>();

    public AuthService(MemberRepository memberRepository,
                       OtpStoreRepository otpStoreRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       JwtUtil jwtUtil) {
        this.memberRepository   = memberRepository;
        this.otpStoreRepository = otpStoreRepository;
        this.passwordEncoder    = passwordEncoder;
        this.emailService       = emailService;
        this.jwtUtil            = jwtUtil;
    }

    // ── STEP 1: Send OTP (Registration) ──────────────────────────

    public String sendOtp(String email) {
        if (email == null || email.isBlank())
            throw new RuntimeException("Email is required.");

        email = email.trim().toLowerCase();

        if (memberRepository.existsByEmail(email))
            throw new RuntimeException("Email is already registered.");

        // Rate limit — 1 OTP per email per 60 seconds
        LocalDateTime lastSent = otpCooldown.get(email);
        if (lastSent != null && lastSent.plusSeconds(OTP_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            long wait = java.time.Duration.between(LocalDateTime.now(),
                    lastSent.plusSeconds(OTP_COOLDOWN_SECONDS)).getSeconds();
            throw new RuntimeException("Please wait " + wait + " seconds before requesting another OTP.");
        }

        String otp = generateOtp();

        // Save OTP first — email failure won't roll back the save
        OtpStore store = otpStoreRepository.findByEmail(email).orElse(new OtpStore());
        store.setEmail(email);
        store.setOtpHash(passwordEncoder.encode(otp));
        store.setExpiry(LocalDateTime.now().plusMinutes(5));
        store.setVerified(false);
        otpStoreRepository.save(store);

       try {
    emailService.sendOtp(email, otp);
} catch (Exception e) {
    throw new RuntimeException(
            "Unable to send OTP. Please try again.");
}

otpCooldown.put(email, LocalDateTime.now());
return "OTP sent to " + email;

    // ── STEP 2: Verify OTP ────────────────────────────────────────

    @Transactional
    public String verifyOtp(String email, String otp) {
        if (email == null || email.isBlank()) throw new RuntimeException("Email is required.");
        if (otp   == null || otp.isBlank())   throw new RuntimeException("OTP is required.");

        email = email.trim().toLowerCase();

        OtpStore store = otpStoreRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("OTP not found. Please request a new one."));

        if (store.getExpiry().isBefore(LocalDateTime.now()))
            throw new RuntimeException("OTP expired. Please request a new one.");

        if (!passwordEncoder.matches(otp, store.getOtpHash()))
            throw new RuntimeException("Invalid OTP. Please try again.");

        store.setVerified(true);
        otpStoreRepository.save(store);
        return "Email verified successfully.";
    }

    // ── STEP 3: Register ──────────────────────────────────────────

    @Transactional
    public String register(RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank())
            throw new RuntimeException("Email is required.");

        String email = request.getEmail().trim().toLowerCase();

        OtpStore store = otpStoreRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Please verify your email with OTP first."));

        if (!store.isVerified())
            throw new RuntimeException("Email not verified. Please complete OTP verification first.");

        if (memberRepository.existsByEmail(email))
            throw new RuntimeException("Email is already registered.");

        if (memberRepository.existsByPhoneNumber(request.getPhoneNumber()))
            throw new RuntimeException("Phone number is already registered.");

      Member member = new Member();

if (request.getName() == null
        || request.getName().trim().length() < 3) {

    throw new RuntimeException(
            "Name must contain at least 3 characters.");
}

member.setName(request.getName().trim());

member.setEmail(email);

if (request.getPhoneNumber() == null
        || !request.getPhoneNumber()
        .matches("^[6-9][0-9]{9}$")) {

    throw new RuntimeException(
            "Invalid mobile number.");
}

member.setPhoneNumber(
        request.getPhoneNumber());

if (request.getPassword() == null
        || request.getPassword().length() < 8) {

    throw new RuntimeException(
            "Password must be at least 8 characters.");
}

member.setPassword(
        passwordEncoder.encode(
                request.getPassword()));

member.setAddress(
        request.getAddress());

member.setStatus(
        Member.Status.PENDING);

member.setRole(
        Member.Role.MEMBER);

member.setEmailVerified(true);

memberRepository.save(member);
        otpStoreRepository.deleteByEmail(email);
        return "Registration successful! Please wait for admin approval.";
    }

    // ── Member Login ──────────────────────────────────────────────

    public LoginResponse memberLogin(String phoneNumber, String password) {
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Invalid phone number or password."));

        if (member.getStatus() == Member.Status.PENDING)
            throw new RuntimeException("Your account is pending admin approval.");

        if (member.getStatus() == Member.Status.REJECTED)
            throw new RuntimeException("Your account has been rejected. Contact admin.");

        if (!passwordEncoder.matches(password, member.getPassword()))
            throw new RuntimeException("Invalid phone number or password.");

        String token = jwtUtil.generateToken(phoneNumber, "MEMBER");
        return new LoginResponse(token, "MEMBER", member.getId(), member.getName());
    }

    // ── Admin Login ───────────────────────────────────────────────

   public LoginResponse adminLogin(String phoneNumber, String password) {

    if (!adminPhone.equals(phoneNumber)
            || !passwordEncoder.matches(password, adminPasswordHash)) {

        throw new RuntimeException("Invalid admin credentials.");
    }

    String token = jwtUtil.generateToken(phoneNumber, "ADMIN");
    return new LoginResponse(token, "ADMIN", 0L, "Admin");
}

    // ─────────────────────────────────────────────────────────────
    // FORGOT PASSWORD — by Email
    // ─────────────────────────────────────────────────────────────

    // Step 1 — Send OTP to the email
    public String forgotPasswordSendOtp(String email) {
        if (email == null || email.isBlank())
            throw new RuntimeException("Email is required.");

        email = email.trim().toLowerCase();

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email address."));

        if (member.getStatus() == Member.Status.REJECTED)
            throw new RuntimeException("Your account has been rejected. Contact admin.");

        // Rate limit for forgot password OTPs too
        LocalDateTime lastSent = otpCooldown.get("fp_" + email);
        if (lastSent != null && lastSent.plusSeconds(OTP_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            long wait = java.time.Duration.between(LocalDateTime.now(),
                    lastSent.plusSeconds(OTP_COOLDOWN_SECONDS)).getSeconds();
            throw new RuntimeException("Please wait " + wait + " seconds before requesting another OTP.");
        }

        String otp = generateOtp();

        OtpStore store = otpStoreRepository.findByEmail(email).orElse(new OtpStore());
        store.setEmail(email);
        store.setOtpHash(passwordEncoder.encode(otp));
        store.setExpiry(LocalDateTime.now().plusMinutes(5));
        store.setVerified(false);
        otpStoreRepository.save(store);

        try {
            emailService.sendForgotPasswordOtp(email, member.getName(), otp);
        } catch (Exception e) {
            throw new RuntimeException("Unable to send OTP. Please try again.");
        }

        otpCooldown.put("fp_" + email, LocalDateTime.now());
        return "OTP sent to " + maskEmail(email);
    }

    // Step 2 — Verify OTP
    @Transactional
    public String forgotPasswordVerifyOtp(String email, String otp) {
        if (email == null || email.isBlank()) throw new RuntimeException("Email is required.");
        if (otp   == null || otp.isBlank())   throw new RuntimeException("OTP is required.");

        email = email.trim().toLowerCase();

        OtpStore store = otpStoreRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("OTP not found. Please request a new one."));

        if (store.getExpiry().isBefore(LocalDateTime.now()))
            throw new RuntimeException("OTP expired. Please request a new one.");

        if (!passwordEncoder.matches(otp, store.getOtpHash()))
            throw new RuntimeException("Invalid OTP. Please try again.");

        store.setVerified(true);
        otpStoreRepository.save(store);
        return "OTP verified. You can now reset your password.";
    }

    // Step 3 — Reset password
    @Transactional
    public String resetPassword(String email, String newPassword) {
        if (email == null || email.isBlank())
            throw new RuntimeException("Email is required.");

        email = email.trim().toLowerCase();

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email address."));

        OtpStore store = otpStoreRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Please verify OTP first."));

        if (!store.isVerified())
            throw new RuntimeException("OTP not verified. Please verify OTP first.");

        if (newPassword == null
        || newPassword.length() < 8) {

    throw new RuntimeException(
            "Password must be at least 8 characters.");
}
        
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);

        otpStoreRepository.deleteByEmail(email);
        // Clear rate limiter for this email
        otpCooldown.remove("fp_" + email);

        return "Password reset successfully! You can now login with your new password.";
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String generateOtp() {
        return String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }
}
