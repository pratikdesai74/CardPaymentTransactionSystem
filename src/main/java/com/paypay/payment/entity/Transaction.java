package com.paypay.payment.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a payment transaction entity.
 */
@Getter
@ToString
public class Transaction {

    private final String transactionId;
    private final String userId;
    private final double amount;

    @Setter
    private double refundedAmount;

    @Setter
    private TransactionStatus status;

    public Transaction(String transactionId, String userId, double amount) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.refundedAmount = 0.0;
        this.status = TransactionStatus.CREATED;
    }

    /**
     * Returns the amount available for refund (captured amount - already refunded).
     */
    public double getRefundableAmount() {
        return amount - refundedAmount;
    }
}
