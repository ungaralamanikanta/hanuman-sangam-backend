package com.sangam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    public enum PaymentStatus { PAID, UNPAID }
    public enum Operation     { ADD, SUBTRACT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Operation operation = Operation.ADD;

    private String note;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @PrePersist
    protected void onCreate() {
        if (this.paymentDate == null) this.paymentDate = LocalDateTime.now();
    }

    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }
    public Member getMember()                       { return member; }
    public void setMember(Member member)            { this.member = member; }
    public Double getAmount()                       { return amount; }
    public void setAmount(Double amount)            { this.amount = amount; }
    public PaymentStatus getPaymentStatus()         { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus ps)  { this.paymentStatus = ps; }
    public Operation getOperation()                 { return operation; }
    public void setOperation(Operation operation)   { this.operation = operation; }
    public String getNote()                         { return note; }
    public void setNote(String note)                { this.note = note; }
    public LocalDateTime getPaymentDate()           { return paymentDate; }
    public void setPaymentDate(LocalDateTime v)     { this.paymentDate = v; }
}
