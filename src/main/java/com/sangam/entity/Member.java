package com.sangam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "members")
public class Member {

    public enum Status { PENDING, APPROVED, REJECTED }
    public enum Role   { MEMBER, ADMIN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, name = "phone_number", nullable = false)
    private String phoneNumber;

    // FIX: @JsonIgnore — password must NEVER be sent to the frontend in any response
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.MEMBER;

    private boolean emailVerified = false;

    @Column(nullable = false)
    private Double totalPaid = 0.0;

    @Column(nullable = false)
    private Double totalPending = 0.0;

    private String lastPaymentNote;
    private LocalDateTime lastPaymentDate;
    private LocalDateTime registeredAt;
    private LocalDateTime approvedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt    = now;
        this.registeredAt = now;
        if (this.status       == null) this.status       = Status.PENDING;
        if (this.role         == null) this.role         = Role.MEMBER;
        if (this.totalPaid    == null) this.totalPaid    = 0.0;
        if (this.totalPending == null) this.totalPending = 0.0;
    }

    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }
    public String getName()                         { return name; }
    public void setName(String name)                { this.name = name; }
    public String getEmail()                        { return email; }
    public void setEmail(String email)              { this.email = email; }
    public String getPhoneNumber()                  { return phoneNumber; }
    public void setPhoneNumber(String p)            { this.phoneNumber = p; }
    public String getPassword()                     { return password; }
    public void setPassword(String password)        { this.password = password; }
    public String getAddress()                      { return address; }
    public void setAddress(String address)          { this.address = address; }
    public Status getStatus()                       { return status; }
    public void setStatus(Status status)            { this.status = status; }
    public Role getRole()                           { return role; }
    public void setRole(Role role)                  { this.role = role; }
    public boolean isEmailVerified()                { return emailVerified; }
    public void setEmailVerified(boolean v)         { this.emailVerified = v; }
    public Double getTotalPaid()                    { return totalPaid != null ? totalPaid : 0.0; }
    public void setTotalPaid(Double v)              { this.totalPaid = v; }
    public Double getTotalPending()                 { return totalPending != null ? totalPending : 0.0; }
    public void setTotalPending(Double v)           { this.totalPending = v; }
    public String getLastPaymentNote()              { return lastPaymentNote; }
    public void setLastPaymentNote(String v)        { this.lastPaymentNote = v; }
    public LocalDateTime getLastPaymentDate()       { return lastPaymentDate; }
    public void setLastPaymentDate(LocalDateTime v) { this.lastPaymentDate = v; }
    public LocalDateTime getRegisteredAt()          { return registeredAt; }
    public void setRegisteredAt(LocalDateTime v)    { this.registeredAt = v; }
    public LocalDateTime getApprovedAt()            { return approvedAt; }
    public void setApprovedAt(LocalDateTime v)      { this.approvedAt = v; }
    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime v)       { this.createdAt = v; }
}
