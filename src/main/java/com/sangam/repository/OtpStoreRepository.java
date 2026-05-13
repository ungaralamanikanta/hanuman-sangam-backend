package com.sangam.repository;

import com.sangam.entity.OtpStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpStoreRepository extends JpaRepository<OtpStore, Long> {

    Optional<OtpStore> findByEmail(String email);

    // ── FIX (Bug 5): Added @Modifying + @Transactional ──
    // Spring Data derived delete queries require @Modifying to execute
    // as a modifying query. Without it, the delete silently fails or throws.
    @Modifying
    @Transactional
    void deleteByEmail(String email);

    // ── FIX (Bug 11): Method to clean up expired OTPs ──
    @Modifying
    @Transactional
    void deleteByExpiryBefore(LocalDateTime cutoff);
}
