package com.sangam.service;

import com.sangam.dto.LoginResponse;
import com.sangam.dto.RegisterRequest;
import com.sangam.entity.Member;
import com.sangam.entity.OtpStore;
import com.sangam.repository.MemberRepository;
import com.sangam.repository.OtpStoreRepository;
import com.sangam.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {

    private final MemberRepository   memberRepository;
    private final OtpStoreRepository otpStoreRepository;
    private final PasswordEncoder    passwordEncoder;
    private final EmailService       emailService;
    private final JwtUtil            jwtUtil;

    // Admin credentials — move to application.properties in production
    private static final String ADMIN_PHONE    = "8985593816";
    private static final String ADMIN_PASSWORD = "admin123";

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

    @Transactional
    public String sendOtp(String email) {
        // Block already-registered emails
        if (memberRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered.");
        }

        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        OtpStore store = otpStoreRepository.findByEmail(email)
                .orElse(new OtpStore());
        store.setEmail(email);
        store.setOtpHash(passwordEncoder.encode(otp));
        store.setExpiry(LocalDateTime.now().plusMinutes(5));
        store.setVerified(false);
        otpStoreRepository.save(store);

        emailService.sendOtp(email, otp);
        return "OTP sent to " + email;
    }

    // ── STEP 2: Verify OTP ───────────────────────────────────────────────────

    @Transactional
    public String verifyOtp(String email, String otp) {
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
        // Ensure OTP was verified
        OtpStore store = otpStoreRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Please verify your email with OTP first."));

        if (!store.isVerified()) {
            throw new RuntimeException("Email not verified. Please complete OTP verification first.");
        }
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered.");
        }
        if (memberRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number is already registered.");
        }

        // BUG FIX: This is the CORE fix for members not appearing in admin dashboard.
        // Status must be explicitly set to PENDING before save so admin can see them.
        Member member = new Member();
        member.setName(request.getName());
        member.setEmail(request.getEmail());
        member.setPhoneNumber(request.getPhoneNumber());
        member.setPassword(passwordEncoder.encode(request.getPassword()));
        member.setAddress(request.getAddress());
        member.setStatus(Member.Status.PENDING);   // ← critical
        member.setRole(Member.Role.MEMBER);
        member.setEmailVerified(true);
        memberRepository.save(member);             // @PrePersist sets createdAt + registeredAt

        // Clean up OTP record
        otpStoreRepository.deleteByEmail(request.getEmail());

        return "Registration successful! Please wait for admin approval.";
    }

    // ── Member Login ─────────────────────────────────────────────────────────

    public LoginResponse memberLogin(String phoneNumber, String password) {
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Invalid phone number or password."));

        // ✅ Check status FIRST, before password
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
        if (!ADMIN_PHONE.equals(phoneNumber) || !ADMIN_PASSWORD.equals(password)) {
            throw new RuntimeException("Invalid admin credentials.");
        }
        String token = jwtUtil.generateToken(phoneNumber, "ADMIN");
        return new LoginResponse(token, "ADMIN", 0L, "Admin");
    }
}
