package com.sangam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_store")
public class OtpStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String otpHash;

    @Column(nullable = false)
    private LocalDateTime expiry;

    private boolean verified = false;

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }
    public String getEmail()                     { return email; }
    public void setEmail(String email)           { this.email = email; }
    public String getOtpHash()                   { return otpHash; }
    public void setOtpHash(String otpHash)       { this.otpHash = otpHash; }
    public LocalDateTime getExpiry()             { return expiry; }
    public void setExpiry(LocalDateTime expiry)  { this.expiry = expiry; }
    public boolean isVerified()                  { return verified; }
    public void setVerified(boolean verified)    { this.verified = verified; }
}
