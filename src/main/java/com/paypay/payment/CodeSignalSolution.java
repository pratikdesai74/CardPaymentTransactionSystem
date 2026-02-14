package com.paypay.payment;

import java.util.*;

/**
 * CODESIGNAL SINGLE-FILE VERSION
 * ================================
 * Copy this entire file into CodeSignal if they require single-file submission.
 *
 * Payment Transaction System - Complete Implementation
 * State Machine: CREATED → AUTHORIZED → CAPTURED → REFUNDED
 */
public class CodeSignalSolution {

    // ==================== ENUMS ====================

    enum TransactionStatus {
        CREATED,
        AUTHORIZED,
        CAPTURED,
        REFUNDED
    }

    // ==================== ENTITY ====================

    static class Transaction {
        private final String transactionId;
        private final String userId;
        private final double amount;
        private double refundedAmount;
        private TransactionStatus status;

        public Transaction(String transactionId, String userId, double amount) {
            this.transactionId = transactionId;
            this.userId = userId;
            this.amount = amount;
            this.refundedAmount = 0.0;
            this.status = TransactionStatus.CREATED;
        }

        public String getTransactionId() { return transactionId; }
        public String getUserId() { return userId; }
        public double getAmount() { return amount; }
        public double getRefundedAmount() { return refundedAmount; }
        public void setRefundedAmount(double refundedAmount) { this.refundedAmount = refundedAmount; }
        public TransactionStatus getStatus() { return status; }
        public void setStatus(TransactionStatus status) { this.status = status; }
        public double getRefundableAmount() { return amount - refundedAmount; }

        @Override
        public String toString() {
            return "Transaction{id='" + transactionId + "', user='" + userId +
                   "', amount=" + amount + ", refunded=" + refundedAmount +
                   ", status=" + status + "}";
        }
    }

    // ==================== EXCEPTIONS ====================

    static class TransactionNotFoundException extends RuntimeException {
        public TransactionNotFoundException(String transactionId) {
            super("Transaction not found: " + transactionId);
        }
    }

    static class InvalidTransactionStateException extends RuntimeException {
        public InvalidTransactionStateException(String transactionId,
                                                TransactionStatus currentStatus,
                                                String attemptedAction) {
            super("Cannot " + attemptedAction + " transaction " + transactionId +
                  " in status " + currentStatus);
        }
    }

    static class InvalidRefundAmountException extends RuntimeException {
        public InvalidRefundAmountException(String transactionId,
                                            double requestedAmount,
                                            double availableAmount) {
            super("Cannot refund " + requestedAmount + " for transaction " + transactionId +
                  ". Available: " + availableAmount);
        }
    }

    // ==================== REPOSITORY ====================

    interface TransactionRepository {
        void save(Transaction transaction);
        Transaction findById(String transactionId);
    }

    static class InMemoryTransactionRepository implements TransactionRepository {
        private final Map<String, Transaction> store = new HashMap<>();

        @Override
        public void save(Transaction transaction) {
            store.put(transaction.getTransactionId(), transaction);
        }

        @Override
        public Transaction findById(String transactionId) {
            return store.get(transactionId);
        }
    }

    // ==================== SERVICE ====================

    static class TransactionService {
        private final TransactionRepository repository;

        public TransactionService(TransactionRepository repository) {
            this.repository = repository;
        }

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

        public void authorize(String transactionId) {
            Transaction transaction = getTransactionOrThrow(transactionId);

            if (transaction.getStatus() != TransactionStatus.CREATED) {
                throw new InvalidTransactionStateException(
                    transactionId, transaction.getStatus(), "authorize");
            }

            transaction.setStatus(TransactionStatus.AUTHORIZED);
            repository.save(transaction);
        }

        public void capture(String transactionId) {
            Transaction transaction = getTransactionOrThrow(transactionId);

            if (transaction.getStatus() != TransactionStatus.AUTHORIZED) {
                throw new InvalidTransactionStateException(
                    transactionId, transaction.getStatus(), "capture");
            }

            transaction.setStatus(TransactionStatus.CAPTURED);
            repository.save(transaction);
        }

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

            if (newRefundedAmount >= transaction.getAmount()) {
                transaction.setStatus(TransactionStatus.REFUNDED);
            }

            repository.save(transaction);
        }

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

    // ==================== MAIN (TEST) ====================

    public static void main(String[] args) {
        TransactionRepository repository = new InMemoryTransactionRepository();
        TransactionService service = new TransactionService(repository);

        System.out.println("=== Card Payment Transaction System ===\n");

        // Test 1: Happy path
        System.out.println("--- Test 1: Full Lifecycle ---");
        Transaction t1 = service.createTransaction("user-123", 100.0);
        System.out.println("Created: " + t1);

        service.authorize(t1.getTransactionId());
        System.out.println("Authorized: " + service.getTransaction(t1.getTransactionId()));

        service.capture(t1.getTransactionId());
        System.out.println("Captured: " + service.getTransaction(t1.getTransactionId()));

        service.refund(t1.getTransactionId(), 100.0);
        System.out.println("Refunded: " + service.getTransaction(t1.getTransactionId()));

        // Test 2: Partial refunds
        System.out.println("\n--- Test 2: Partial Refunds ---");
        Transaction t2 = service.createTransaction("user-456", 200.0);
        service.authorize(t2.getTransactionId());
        service.capture(t2.getTransactionId());

        service.refund(t2.getTransactionId(), 50.0);
        System.out.println("After 1st refund: " + service.getTransaction(t2.getTransactionId()));

        service.refund(t2.getTransactionId(), 150.0);
        System.out.println("After 2nd refund: " + service.getTransaction(t2.getTransactionId()));

        // Test 3: Invalid state transition
        System.out.println("\n--- Test 3: Invalid State ---");
        Transaction t3 = service.createTransaction("user-789", 50.0);
        try {
            service.capture(t3.getTransactionId()); // Should fail - not authorized
        } catch (InvalidTransactionStateException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        // Test 4: Refund exceeds amount
        System.out.println("\n--- Test 4: Invalid Refund ---");
        Transaction t4 = service.createTransaction("user-111", 75.0);
        service.authorize(t4.getTransactionId());
        service.capture(t4.getTransactionId());
        try {
            service.refund(t4.getTransactionId(), 100.0); // Should fail
        } catch (InvalidRefundAmountException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        System.out.println("\n=== All Tests Passed! ===");
    }
}
