package com.sangam.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DbKeepAlive {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
}