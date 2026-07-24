package com.sangam.service;

import com.sangam.dto.AdminUpdateMemberRequest;
import com.sangam.dto.PaymentRequest;
import com.sangam.dto.StatsUpdateRequest;
import com.sangam.dto.StatsUpdateRequest;
import com.sangam.entity.Announcement;
import com.sangam.entity.Member;
import com.sangam.entity.Payment;
import com.sangam.repository.AnnouncementRepository;
import com.sangam.repository.MemberRepository;
import com.sangam.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final MemberRepository       memberRepository;
    private final PaymentRepository      paymentRepository;
    private final AnnouncementRepository announcementRepository;
    private final EmailService           emailService;
    private final DashboardStatsService  statsService;

    public AdminService(MemberRepository memberRepository,
                        PaymentRepository paymentRepository,
                        AnnouncementRepository announcementRepository,
                        EmailService emailService,
                        DashboardStatsService statsService) {
        this.memberRepository       = memberRepository;
        this.paymentRepository      = paymentRepository;
        this.announcementRepository = announcementRepository;
        this.emailService           = emailService;
        this.statsService           = statsService;
    }

    // ── Member Management ─────────────────────────────────────────

    public List<Member> getAllMembers()      { return memberRepository.findAll(); }
    public List<Member> getPendingMembers()  { return memberRepository.findByStatus(Member.Status.PENDING); }
    public List<Member> getApprovedMembers(){ return memberRepository.findByStatus(Member.Status.APPROVED); }

    @Transactional
    public Member approveMember(Long memberId) {
        Member member = findMember(memberId);
        member.setStatus(Member.Status.APPROVED);
        member.setApprovedAt(LocalDateTime.now());
        memberRepository.save(member);
        // Auto-update stats when member count changes
        autoUpdateStats();
        try { emailService.sendApprovalNotification(member.getEmail(), member.getName()); }
        catch (Exception ignored) {}
        return member;
    }

    @Transactional
    public Member rejectMember(Long memberId) {
        Member member = findMember(memberId);
        member.setStatus(Member.Status.REJECTED);
        memberRepository.save(member);
        // Auto-update stats
        autoUpdateStats();
        try { emailService.sendRejectionNotification(member.getEmail(), member.getName()); }
        catch (Exception ignored) {}
        return member;
    }

    @Transactional
    public void deleteMember(Long memberId) {
        memberRepository.delete(findMember(memberId));
        // Auto-update stats after delete
        autoUpdateStats();
    }

    @Transactional
    public Member updateMemberProfile(Long memberId, AdminUpdateMemberRequest req) {
        Member member = findMember(memberId);
        if (req.getName()        != null) member.setName(req.getName());
        if (req.getEmail()       != null) member.setEmail(req.getEmail());
        if (req.getPhoneNumber() != null) member.setPhoneNumber(req.getPhoneNumber());
        if (req.getAddress()     != null) member.setAddress(req.getAddress());
        return memberRepository.save(member);
    }

    // ── Payment Management ────────────────────────────────────────

    @Transactional
    public Payment addPayment(Long memberId, PaymentRequest req) {
        Member member = findMember(memberId);

        double amount = req.getAmount();
        Payment.Operation     op     = req.getOperation();
        Payment.PaymentStatus status = req.getPaymentStatus();

        if (op == Payment.Operation.ADD) {
            if (status == Payment.PaymentStatus.PAID) {
                member.setTotalPaid(member.getTotalPaid() + amount);
            } else {
                member.setTotalPending(member.getTotalPending() + amount);
            }
        } else { // SUBTRACT
            if (status == Payment.PaymentStatus.PAID) {
                member.setTotalPaid(Math.max(0, member.getTotalPaid() - amount));
            } else {
                member.setTotalPending(Math.max(0, member.getTotalPending() - amount));
            }
        }

        member.setLastPaymentNote(req.getNote());
        member.setLastPaymentDate(LocalDateTime.now());
        memberRepository.save(member);

        Payment payment = new Payment();
        payment.setMember(member);
        payment.setAmount(amount);
        payment.setPaymentStatus(status);
        payment.setOperation(op);
        payment.setNote(req.getNote());
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        // ✅ Auto-update DashboardStats after every payment
        // This makes home page + member dashboard reflect changes instantly
        autoUpdateStats();

        return payment;
    }

    public List<Payment> getAllPayments()                    { return paymentRepository.findAllByOrderByPaymentDateDesc(); }
    public List<Payment> getPaymentsByMember(Long memberId) { return paymentRepository.findByMemberId(memberId); }

    @Transactional
    public void deletePayment(Long paymentId) {
        paymentRepository.deleteById(paymentId);
        // Auto-update stats after payment delete too
        autoUpdateStats();
    }

    // ── Auto Stats Update (called after every payment/approval) ──

    /**
     * Automatically recalculates and saves DashboardStats
     * from live member data after any payment or approval change.
     * This ensures home page (/admin/stats) and member dashboard
     * always show real-time values without manual "Save" click.
     */
    private void autoUpdateStats() {
        try {
            // Just pass autoCalculate=true — DashboardStatsService
            // uses memberRepository.sumTotalPaidApproved() internally
            StatsUpdateRequest req = new StatsUpdateRequest();
            req.setAutoCalculate(true);
            statsService.updateStats(req);
        } catch (Exception ignored) {
            // Silent — don't break payment flow if stats update fails
        }
    }

    // ── Dashboard Stats ───────────────────────────────────────────

    public Map<String, Object> getDashboardStats(String month) {
        List<Member> approvedMembers = memberRepository.findByStatus(Member.Status.APPROVED);
        List<Member> pendingMembers  = memberRepository.findByStatus(Member.Status.PENDING);

        List<Map<String, Object>> memberRows = new ArrayList<>();
        double totalPaidAll    = 0;
        double totalPendingAll = 0;

        for (Member m : approvedMembers) {
            double paid    = m.getTotalPaid();
            double pending = m.getTotalPending();
            totalPaidAll    += paid;
            totalPendingAll += pending;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",      m.getId());
            row.put("name",    m.getName());
            row.put("email",   m.getEmail());
            row.put("paid",    paid);
            row.put("pending", pending);
            row.put("contact", m.getPhoneNumber());
            row.put("status",  m.getStatus());
            row.put("address", m.getAddress());
            memberRows.add(row);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCollection", totalPaidAll);
        stats.put("totalPending",    totalPendingAll);
        stats.put("pendingMembers",  pendingMembers.size());
        stats.put("members",         memberRows);
        return stats;
    }

    // ── Announcements ─────────────────────────────────────────────

    @Transactional
    public Announcement sendAnnouncement(String title, String message) {
        Announcement ann = new Announcement();
        ann.setTitle(title);
        ann.setMessage(message);
        announcementRepository.save(ann);
        memberRepository.findByStatus(Member.Status.APPROVED).forEach(m -> {
            try { emailService.sendAnnouncementEmail(m.getEmail(), m.getName(), title, message); }
            catch (Exception ignored) {}
        });
        return ann;
    }

    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void deleteAnnouncement(Long id) { announcementRepository.deleteById(id); }

    private Member findMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found: " + id));
    }
}
