package com.sangam.controller;

import com.sangam.entity.Member;
import com.sangam.entity.Payment;
import com.sangam.repository.MemberRepository;
import com.sangam.repository.PaymentRepository;
import com.sangam.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/member")
// FIX: origins="*" blocks requests with Authorization header in browsers.
// Must list explicit origins including "null" for file:// pages.
@CrossOrigin(
    origins = {
        "http://localhost:8081",
        "http://localhost:8080",
        "http://127.0.0.1:8081",
        "http://127.0.0.1:8080",
        "null"  // ← browsers send Origin: null for file:// opened pages
    },
    allowedHeaders = "*",
    methods = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.OPTIONS
    },
    allowCredentials = "false"
)
public class MemberController {

    private final MemberService     memberService;
    private final PaymentRepository paymentRepository;
    private final MemberRepository  memberRepository;

    public MemberController(MemberService memberService,
                            PaymentRepository paymentRepository,
                            MemberRepository memberRepository) {
        this.memberService     = memberService;
        this.paymentRepository = paymentRepository;
        this.memberRepository  = memberRepository;
    }

    // ── My Profile ────────────────────────────────────────────────
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal String phoneNumber) {
        try {
            return ResponseEntity.ok(memberService.getProfile(phoneNumber));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── My Payment History ────────────────────────────────────────
    @GetMapping("/payments")
    public ResponseEntity<?> getMyPayments(@AuthenticationPrincipal String phoneNumber) {
        try {
            Member member   = memberService.getProfile(phoneNumber);
            List<Payment> p = paymentRepository.findByMemberId(member.getId());
            return ResponseEntity.ok(p);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── All Approved Members ──────────────────────────────────────
    // Returns a plain JSON array: [ {id, name, paid, pending, contact}, ... ]
    // FIX (frontend): member-dashboard.js was reading data.members but this
    // endpoint returns a plain array — fixed in member-dashboard.js to use
    // Array.isArray(data) ? data : [] instead of data.members
    @GetMapping("/members")
    public ResponseEntity<?> getAllApprovedMembers() {
        try {
            List<Member> approved = memberRepository.findByStatus(Member.Status.APPROVED);
            List<Map<String, Object>> result = new ArrayList<>();

            for (Member m : approved) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id",      m.getId());
                row.put("name",    m.getName());
                row.put("paid",    m.getTotalPaid());
                row.put("pending", m.getTotalPending());
                row.put("contact", m.getPhoneNumber());
                result.add(row);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}