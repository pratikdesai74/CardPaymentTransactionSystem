package com.paypay.payment.repository;

import com.paypay.payment.entity.Transaction;

/**
 * Repository interface for Transaction persistence operations.
 */
public interface TransactionRepository {

    void save(Transaction transaction);

    Transaction findById(String transactionId);

    boolean exists(String transactionId);
}
