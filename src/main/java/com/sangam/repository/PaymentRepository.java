package com.sangam.repository;

import com.sangam.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // ── FIX 1: Derived findByMemberId() fails — Payment has no "memberId" field,
    // only a "member" relationship. Use explicit JPQL to navigate p.member.id ──
    @Query("SELECT p FROM Payment p WHERE p.member.id = :memberId ORDER BY p.paymentDate DESC")
    List<Payment> findByMemberId(@Param("memberId") Long memberId);

    List<Payment> findAllByOrderByPaymentDateDesc();

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p " +
           "WHERE p.paymentStatus = com.sangam.entity.Payment.PaymentStatus.PAID")
    double getTotalPaidAllTime();

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p " +
           "WHERE p.paymentStatus = com.sangam.entity.Payment.PaymentStatus.UNPAID")
    double getTotalPendingAllTime();

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p " +
           "WHERE p.member.id = :memberId " +
           "AND p.paymentStatus = com.sangam.entity.Payment.PaymentStatus.PAID")
    double getMemberPaidAllTime(@Param("memberId") Long memberId);

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p " +
           "WHERE p.member.id = :memberId " +
           "AND p.paymentStatus = com.sangam.entity.Payment.PaymentStatus.UNPAID")
    double getMemberPendingAllTime(@Param("memberId") Long memberId);

    // ── FIX 2: DATE_FORMAT() is MySQL syntax — replaced with TO_CHAR() for PostgreSQL ──
    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p " +
           "WHERE p.paymentStatus = com.sangam.entity.Payment.PaymentStatus.PAID " +
           "AND TO_CHAR(p.paymentDate, 'YYYY-MM') = :month")
    double getTotalPaidByMonth(@Param("month") String month);

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p " +
           "WHERE p.paymentStatus = com.sangam.entity.Payment.PaymentStatus.UNPAID " +
           "AND TO_CHAR(p.paymentDate, 'YYYY-MM') = :month")
    double getTotalPendingByMonth(@Param("month") String month);

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p " +
           "WHERE p.member.id = :memberId " +
           "AND p.paymentStatus = com.sangam.entity.Payment.PaymentStatus.PAID " +
           "AND TO_CHAR(p.paymentDate, 'YYYY-MM') = :month")
    double getMemberPaidByMonth(@Param("memberId") Long memberId, @Param("month") String month);

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p " +
           "WHERE p.member.id = :memberId " +
           "AND p.paymentStatus = com.sangam.entity.Payment.PaymentStatus.UNPAID " +
           "AND TO_CHAR(p.paymentDate, 'YYYY-MM') = :month")
    double getMemberPendingByMonth(@Param("memberId") Long memberId, @Param("month") String month);

    @Query("SELECT DISTINCT TO_CHAR(p.paymentDate, 'YYYY-MM') " +
           "FROM Payment p ORDER BY 1 DESC")
    List<String> findDistinctPaymentMonths();
}
