package com.sangam.repository;

import com.sangam.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByMemberId(Long memberId);
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

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p " +
           "WHERE p.paymentStatus = com.sangam.entity.Payment.PaymentStatus.PAID " +
           "AND FUNCTION('DATE_FORMAT', p.paymentDate, '%Y-%m') = :month")
    double getTotalPaidByMonth(@Param("month") String month);

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p " +
           "WHERE p.paymentStatus = com.sangam.entity.Payment.PaymentStatus.UNPAID " +
           "AND FUNCTION('DATE_FORMAT', p.paymentDate, '%Y-%m') = :month")
    double getTotalPendingByMonth(@Param("month") String month);

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p " +
           "WHERE p.member.id = :memberId " +
           "AND p.paymentStatus = com.sangam.entity.Payment.PaymentStatus.PAID " +
           "AND FUNCTION('DATE_FORMAT', p.paymentDate, '%Y-%m') = :month")
    double getMemberPaidByMonth(@Param("memberId") Long memberId, @Param("month") String month);

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p " +
           "WHERE p.member.id = :memberId " +
           "AND p.paymentStatus = com.sangam.entity.Payment.PaymentStatus.UNPAID " +
           "AND FUNCTION('DATE_FORMAT', p.paymentDate, '%Y-%m') = :month")
    double getMemberPendingByMonth(@Param("memberId") Long memberId, @Param("month") String month);

    @Query("SELECT DISTINCT FUNCTION('DATE_FORMAT', p.paymentDate, '%Y-%m') " +
           "FROM Payment p ORDER BY 1 DESC")
    List<String> findDistinctPaymentMonths();
}
