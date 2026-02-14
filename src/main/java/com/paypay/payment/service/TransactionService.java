package com.paypay.payment.service;

import com.paypay.payment.entity.Transaction;
import com.paypay.payment.entity.TransactionStatus;
import com.paypay.payment.exception.InvalidRefundAmountException;
import com.paypay.payment.exception.InvalidTransactionStateException;
import com.paypay.payment.exception.TransactionNotFoundException;
import com.paypay.payment.repository.TransactionRepository;

import java.util.UUID;

/**
 * Service layer for managing payment transaction lifecycle.
 *
 * State transitions:
 * CREATED -> AUTHORIZED -> CAPTURED -> REFUNDED (terminal)
 */
public class TransactionService {

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates a new transaction in CREATED status.
     */
    public Transaction createTransaction(String userId, double amount) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = new Transaction(transactionId, userId, amount);
        repository.save(transaction);
        return transaction;
    }

    /**
     * Authorizes a transaction.
     * Valid only when transaction is in CREATED status.
     */
    public void authorize(String transactionId) {
        Transaction transaction = getTransactionOrThrow(transactionId);

        if (transaction.getStatus() != TransactionStatus.CREATED) {
            throw new InvalidTransactionStateException(
                transactionId, transaction.getStatus(), "authorize");
        }

        transaction.setStatus(TransactionStatus.AUTHORIZED);
        repository.save(transaction);
    }

    /**
     * Captures an authorized transaction.
     * Valid only when transaction is in AUTHORIZED status.
     */
    public void capture(String transactionId) {
        Transaction transaction = getTransactionOrThrow(transactionId);

        if (transaction.getStatus() != TransactionStatus.AUTHORIZED) {
            throw new InvalidTransactionStateException(
                transactionId, transaction.getStatus(), "capture");
        }

        transaction.setStatus(TransactionStatus.CAPTURED);
        repository.save(transaction);
    }

    /**
     * Refunds a captured transaction (partial or full).
     * Valid only when transaction is in CAPTURED status.
     * Total refunded amount cannot exceed the original captured amount.
     */
    public void refund(String transactionId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("refund amount must be positive");
        }

        Transaction transaction = getTransactionOrThrow(transactionId);

        if (transaction.getStatus() != TransactionStatus.CAPTURED) {
            throw new InvalidTransactionStateException(
                transactionId, transaction.getStatus(), "refund");
        }

        double availableForRefund = transaction.getRefundableAmount();
        if (amount > availableForRefund) {
            throw new InvalidRefundAmountException(
                transactionId, amount, availableForRefund);
        }

        double newRefundedAmount = transaction.getRefundedAmount() + amount;
        transaction.setRefundedAmount(newRefundedAmount);

        // Mark as REFUNDED only when fully refunded
        if (newRefundedAmount >= transaction.getAmount()) {
            transaction.setStatus(TransactionStatus.REFUNDED);
        }

        repository.save(transaction);
    }

    /**
     * Retrieves transaction details.
     */
    public Transaction getTransaction(String transactionId) {
        return getTransactionOrThrow(transactionId);
    }

    private Transaction getTransactionOrThrow(String transactionId) {
        Transaction transaction = repository.findById(transactionId);
        if (transaction == null) {
            throw new TransactionNotFoundException(transactionId);
        }
        return transaction;
    }
}
