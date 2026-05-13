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

    // ── FIX (Bug 8): Admin credentials from environment/config, not hardcoded ──
    @Value("${app.admin.phone}")
    private String adminPhone;

    @Value("${app.admin.password}")
    private String adminPassword;

    // ── FIX (Bug 7): Use SecureRandom instead of java.util.Random ──
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ── FIX (Bug 6): In-memory rate limiter for OTP requests ──
    private final Map<String, LocalDateTime> otpCooldown = new ConcurrentHashMap<>();
    private static final int OTP_COOLDOWN_SECONDS = 60;

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

    // ── STEP 1: Send OTP ──────────────────────────────────────────────────────
    // FIX (Bug 1 & 3): Removed @Transactional so email failure does NOT
    // roll back the OTP save. The OTP is persisted first, then email is sent.
    // If email fails, OTP remains in DB and user can retry.

    public String sendOtp(String email) {
        // ── FIX (Bug 2): Null/blank check (also done in controller, defense in depth) ──
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required.");
        }
        email = email.trim().toLowerCase();

        // Block already-registered emails
        if (memberRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered.");
        }

        // ── FIX (Bug 6): Rate limit — 1 OTP per email per 60 seconds ──
        LocalDateTime lastSent = otpCooldown.get(email);
        if (lastSent != null && lastSent.plusSeconds(OTP_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            long waitSeconds = java.time.Duration.between(LocalDateTime.now(),
                    lastSent.plusSeconds(OTP_COOLDOWN_SECONDS)).getSeconds();
            throw new RuntimeException("Please wait " + waitSeconds + " seconds before requesting another OTP.");
        }

        // ── FIX (Bug 7): SecureRandom for cryptographic OTP generation ──
        String otp = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));

        // Save OTP to DB first (no @Transactional, so this commits immediately)
        OtpStore store = otpStoreRepository.findByEmail(email)
                .orElse(new OtpStore());
        store.setEmail(email);
        store.setOtpHash(passwordEncoder.encode(otp));
        store.setExpiry(LocalDateTime.now().plusMinutes(5));
        store.setVerified(false);
        otpStoreRepository.save(store);

        // ── FIX (Bug 1): Send email AFTER DB save, outside transaction ──
        // If this fails, the OTP is still in DB — user can click "Resend OTP"
        try {
            emailService.sendOtp(email, otp);
        } catch (Exception e) {
            // OTP is saved but email failed — user should retry
            throw new RuntimeException(
                "OTP generated but email delivery failed. Please click 'Resend OTP'. Error: " + e.getMessage());
        }

        // Update cooldown tracker
        otpCooldown.put(email, LocalDateTime.now());

        return "OTP sent to " + email;
    }

    // ── STEP 2: Verify OTP ───────────────────────────────────────────────────

    @Transactional
    public String verifyOtp(String email, String otp) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required.");
        }
        if (otp == null || otp.isBlank()) {
            throw new RuntimeException("OTP is required.");
        }
        email = email.trim().toLowerCase();

        OtpStore store = otpStoreRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("OTP not found. Please request a new one."));

        if (store.getExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired. Please request a new one.");
        }
        if (!passwordEncoder.matches(otp, store.getOtpHash())) {
            throw new RuntimeException("Invalid OTP. Please try again.");
        }

        store.setVerified(true);
        otpStoreRepository.save(store);
        return "Email verified successfully.";
    }

    // ── STEP 3: Complete Registration ────────────────────────────────────────

    @Transactional
    public String register(RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Email is required.");
        }
        String email = request.getEmail().trim().toLowerCase();

        // Ensure OTP was verified
        OtpStore store = otpStoreRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Please verify your email with OTP first."));

        if (!store.isVerified()) {
            throw new RuntimeException("Email not verified. Please complete OTP verification first.");
        }
        if (memberRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered.");
        }
        if (memberRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number is already registered.");
        }

        Member member = new Member();
        member.setName(request.getName());
        member.setEmail(email);
        member.setPhoneNumber(request.getPhoneNumber());
        member.setPassword(passwordEncoder.encode(request.getPassword()));
        member.setAddress(request.getAddress());
        member.setStatus(Member.Status.PENDING);
        member.setRole(Member.Role.MEMBER);
        member.setEmailVerified(true);
        memberRepository.save(member);

        // Clean up OTP record
        otpStoreRepository.deleteByEmail(email);

        return "Registration successful! Please wait for admin approval.";
    }

    // ── Member Login ─────────────────────────────────────────────────────────

    public LoginResponse memberLogin(String phoneNumber, String password) {
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Invalid phone number or password."));

        if (member.getStatus() == Member.Status.PENDING) {
            throw new RuntimeException("Your account is pending admin approval.");
        }
        if (member.getStatus() == Member.Status.REJECTED) {
            throw new RuntimeException("Your account has been rejected. Contact admin.");
        }

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new RuntimeException("Invalid phone number or password.");
        }

        String token = jwtUtil.generateToken(phoneNumber, "MEMBER");
        return new LoginResponse(token, "MEMBER", member.getId(), member.getName());
    }

    // ── Admin Login ──────────────────────────────────────────────────────────

    public LoginResponse adminLogin(String phoneNumber, String password) {
        if (!adminPhone.equals(phoneNumber) || !adminPassword.equals(password)) {
            throw new RuntimeException("Invalid admin credentials.");
        }
        String token = jwtUtil.generateToken(phoneNumber, "ADMIN");
        return new LoginResponse(token, "ADMIN", 0L, "Admin");
    }
}
