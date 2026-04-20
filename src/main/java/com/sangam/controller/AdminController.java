package com.sangam.controller;

import com.sangam.dto.AdminUpdateMemberRequest;
import com.sangam.dto.AnnouncementRequest;
import com.sangam.dto.PaymentRequest;
import com.sangam.dto.StatsUpdateRequest;
import com.sangam.entity.Announcement;
import com.sangam.entity.DashboardStats;
import com.sangam.entity.Member;
import com.sangam.entity.Payment;
import com.sangam.service.AdminService;
import com.sangam.service.DashboardStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    // BUG FIX: original controller injected MemberService for approve/reject.
    // MemberService.approveMember() never set approvedAt and never sent email.
    // All admin actions now go through AdminService which handles both correctly.
    private final AdminService          adminService;
    private final DashboardStatsService statsService;

    public AdminController(AdminService adminService,
                           DashboardStatsService statsService) {
        this.adminService = adminService;
        this.statsService = statsService;
    }

    // ── Members ──────────────────────────────────────────────────────────────

    @GetMapping("/members")
    public List<Member> getAllMembers() {
        return adminService.getAllMembers();
    }

    @GetMapping("/members/approved")
    public List<Member> getApprovedMembers() {
        return adminService.getApprovedMembers();
    }

    /** This is what the admin dashboard page calls to show the pending list */
    @GetMapping("/members/pending")
    public List<Member> getPendingMembers() {
        return adminService.getPendingMembers();
    }

    @PutMapping("/members/{id}/approve")
    public ResponseEntity<?> approveMember(@PathVariable Long id) {
        try {
            Member m = adminService.approveMember(id);
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/members/{id}/reject")
    public ResponseEntity<?> rejectMember(@PathVariable Long id) {
        try {
            Member m = adminService.rejectMember(id);
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/members/{id}")
    public ResponseEntity<?> updateMember(@PathVariable Long id,
                                          @RequestBody AdminUpdateMemberRequest req) {
        try {
            Member m = adminService.updateMemberProfile(id, req);
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/members/{id}")
    public ResponseEntity<?> deleteMember(@PathVariable Long id) {
        try {
            adminService.deleteMember(id);
            return ResponseEntity.ok(Map.of("message", "Member deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Payments ─────────────────────────────────────────────────────────────

    @PostMapping("/payments")
    public ResponseEntity<?> recordPayment(@RequestBody PaymentRequest req) {
        try {
            Payment p = adminService.addPayment(req.getMemberId(), req);
            return ResponseEntity.ok(p);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/payments")
    public List<Payment> getAllPayments() {
        return adminService.getAllPayments();
    }

    @GetMapping("/payments/member/{memberId}")
    public List<Payment> getPaymentsForMember(@PathVariable Long memberId) {
        return adminService.getPaymentsByMember(memberId);
    }

    @DeleteMapping("/payments/{id}")
    public ResponseEntity<?> deletePayment(@PathVariable Long id) {
        try {
            adminService.deletePayment(id);
            return ResponseEntity.ok(Map.of("message", "Payment deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Dashboard Stats ───────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(adminService.getDashboardStats(month));
    }

    @GetMapping("/stats")
    public DashboardStats getStats() {
        return statsService.getStats();
    }

    @PutMapping("/stats")
    public DashboardStats updateStats(@RequestBody StatsUpdateRequest req) {
        return statsService.updateStats(req);
    }

    // ── Announcements ─────────────────────────────────────────────────────────

    @PostMapping("/announcements")
    public ResponseEntity<?> sendAnnouncement(@RequestBody AnnouncementRequest req) {
        try {
            Announcement a = adminService.sendAnnouncement(req.getTitle(), req.getMessage());
            return ResponseEntity.ok(a);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/announcements")
    public List<Announcement> getAllAnnouncements() {
        return adminService.getAllAnnouncements();
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id) {
        try {
            adminService.deleteAnnouncement(id);
            return ResponseEntity.ok(Map.of("message", "Announcement deleted."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
