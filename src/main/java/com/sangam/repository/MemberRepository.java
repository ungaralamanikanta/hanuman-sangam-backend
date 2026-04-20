package com.sangam.repository;

import com.sangam.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByStatus(Member.Status status);
    long countByStatus(Member.Status status);

    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<Member> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT COALESCE(SUM(m.totalPaid), 0) FROM Member m " +
           "WHERE m.status = com.sangam.entity.Member.Status.APPROVED")
    Double sumTotalPaidApproved();

    @Query("SELECT COALESCE(SUM(m.totalPending), 0) FROM Member m " +
           "WHERE m.status = com.sangam.entity.Member.Status.APPROVED")
    Double sumTotalPendingApproved();
}
