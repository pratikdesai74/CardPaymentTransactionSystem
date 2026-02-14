package com.paypay.payment.entity;

/**
 * Represents the lifecycle states of a payment transaction.
 *
 * Valid transitions:
 * CREATED -> AUTHORIZED -> CAPTURED -> REFUNDED (terminal)
 */
public enum TransactionStatus {
    CREATED,
    AUTHORIZED,
    CAPTURED,
    REFUNDED
}
