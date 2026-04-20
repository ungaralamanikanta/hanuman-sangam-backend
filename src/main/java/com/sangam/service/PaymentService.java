package com.sangam.service;

import com.sangam.dto.PaymentRequest;
import com.sangam.entity.Member;
import com.sangam.entity.Payment;
import com.sangam.repository.MemberRepository;
import com.sangam.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository  memberRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          MemberRepository memberRepository) {
        this.paymentRepository = paymentRepository;
        this.memberRepository  = memberRepository;
    }

    @Transactional
    public Payment recordPayment(PaymentRequest req) {
        Member member = memberRepository.findById(req.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found: " + req.getMemberId()));

        // BUG FIX: Keep as enums — do NOT call .toUpperCase() (that's a String method).
        // Do NOT pass to setters as String — setters expect enum types.
        Payment.Operation     op     = req.getOperation();
        Payment.PaymentStatus status = req.getPaymentStatus();
        double amount = req.getAmount();

        // Update member running totals
        if (op == Payment.Operation.ADD) {
            if (status == Payment.PaymentStatus.PAID) {
                member.setTotalPaid(member.getTotalPaid() + amount);
            } else {
                member.setTotalPending(member.getTotalPending() + amount);
            }
        } else if (op == Payment.Operation.SUBTRACT) {
            if (status == Payment.PaymentStatus.PAID) {
                member.setTotalPaid(Math.max(0, member.getTotalPaid() - amount));
            } else {
                member.setTotalPending(Math.max(0, member.getTotalPending() - amount));
            }
        }
        memberRepository.save(member);

        // Save payment record
        Payment payment = new Payment();
        payment.setMember(member);
        payment.setAmount(amount);
        payment.setPaymentStatus(status);  // enum → enum ✅
        payment.setOperation(op);          // enum → enum ✅
        payment.setNote(req.getNote());

        return paymentRepository.save(payment);
    }

    public List<Payment> getPaymentsByMember(Long memberId) {
        return paymentRepository.findByMemberId(memberId);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByPaymentDateDesc();
    }
}
