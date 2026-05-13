package com.sangam.scheduler;

import com.sangam.repository.OtpStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DbKeepAlive {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OtpStoreRepository otpStoreRepository;

    // Ping DB every 4 minutes to prevent Supabase from sleeping
    @Scheduled(fixedRate = 240000)
    public void keepAlive() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            System.out.println("✅ DB keep-alive ping successful");
        } catch (Exception e) {
            System.err.println("❌ DB keep-alive failed: " + e.getMessage());
        }
    }

    // ── FIX (Bug 11): Clean up expired OTPs every hour ──
    // Expired OTP records pile up in otp_store forever without this.
    // Cleans any OTP that expired more than 1 hour ago.
    @Scheduled(fixedRate = 3600000)
    public void cleanExpiredOtps() {
        try {
            otpStoreRepository.deleteByExpiryBefore(LocalDateTime.now().minusHours(1));
            System.out.println("✅ Expired OTPs cleaned up");
        } catch (Exception e) {
            System.err.println("❌ OTP cleanup failed: " + e.getMessage());
        }
    }
}
