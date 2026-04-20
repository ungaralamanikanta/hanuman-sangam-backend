package com.sangam.dto;

import com.sangam.entity.Payment;

public class PaymentRequest {
    private Long memberId;
    private Double amount;
    private Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.PAID;
    private Payment.Operation operation         = Payment.Operation.ADD;
    private String note;

    public Long getMemberId()                              { return memberId; }
    public void setMemberId(Long memberId)                 { this.memberId = memberId; }
    public Double getAmount()                              { return amount; }
    public void setAmount(Double amount)                   { this.amount = amount; }
    public Payment.PaymentStatus getPaymentStatus()        { return paymentStatus; }
    public void setPaymentStatus(Payment.PaymentStatus ps) { this.paymentStatus = ps; }
    public Payment.Operation getOperation()                { return operation; }
    public void setOperation(Payment.Operation o)          { this.operation = o; }
    public String getNote()                                { return note; }
    public void setNote(String note)                       { this.note = note; }
}
