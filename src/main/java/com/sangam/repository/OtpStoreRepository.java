package com.sangam.repository;

import com.sangam.entity.OtpStore;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpStoreRepository extends JpaRepository<OtpStore, Long> {
    Optional<OtpStore> findByEmail(String email);
    void deleteByEmail(String email);
}
