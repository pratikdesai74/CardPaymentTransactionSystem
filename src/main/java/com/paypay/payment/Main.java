package com.paypay.payment;

import com.paypay.payment.entity.Transaction;
import com.paypay.payment.exception.InvalidRefundAmountException;
import com.paypay.payment.exception.InvalidTransactionStateException;
import com.paypay.payment.exception.TransactionNotFoundException;
import com.paypay.payment.repository.InMemoryTransactionRepository;
import com.paypay.payment.repository.TransactionRepository;
import com.paypay.payment.service.TransactionService;

/**
 * Main class demonstrating the Payment Transaction System.
 */
public class Main {

    public static void main(String[] args) {
        TransactionRepository repository = new InMemoryTransactionRepository();
        TransactionService service = new TransactionService(repository);

        System.out.println("=== Payment Transaction System Demo ===\n");

        // Test 1: Happy path - full lifecycle
        System.out.println("--- Test 1: Happy Path (Full Lifecycle) ---");
        Transaction t1 = service.createTransaction("user-123", 100.00);
        System.out.println("Created: " + t1);

        service.authorize(t1.getTransactionId());
        System.out.println("Authorized: " + service.getTransaction(t1.getTransactionId()));

        service.capture(t1.getTransactionId());
        System.out.println("Captured: " + service.getTransaction(t1.getTransactionId()));

        service.refund(t1.getTransactionId(), 100.00);
        System.out.println("Refunded: " + service.getTransaction(t1.getTransactionId()));
        System.out.println();

        // Test 2: Partial refunds
        System.out.println("--- Test 2: Partial Refunds ---");
        Transaction t2 = service.createTransaction("user-456", 200.00);
        service.authorize(t2.getTransactionId());
        service.capture(t2.getTransactionId());
        System.out.println("After capture: " + service.getTransaction(t2.getTransactionId()));

        service.refund(t2.getTransactionId(), 50.00);
        System.out.println("After 1st refund (50): " + service.getTransaction(t2.getTransactionId()));

        service.refund(t2.getTransactionId(), 100.00);
        System.out.println("After 2nd refund (100): " + service.getTransaction(t2.getTransactionId()));

        service.refund(t2.getTransactionId(), 50.00);
        System.out.println("After 3rd refund (50 - fully refunded): " + service.getTransaction(t2.getTransactionId()));
        System.out.println();

        // Test 3: Invalid state transition - authorize already authorized
        System.out.println("--- Test 3: Invalid State Transition ---");
        Transaction t3 = service.createTransaction("user-789", 50.00);
        service.authorize(t3.getTransactionId());
        try {
            service.authorize(t3.getTransactionId()); // Should fail
            System.out.println("ERROR: Should have thrown exception!");
        } catch (InvalidTransactionStateException e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }
        System.out.println();

        // Test 4: Invalid refund - exceeds available amount
        System.out.println("--- Test 4: Invalid Refund Amount ---");
        Transaction t4 = service.createTransaction("user-111", 75.00);
        service.authorize(t4.getTransactionId());
        service.capture(t4.getTransactionId());
        try {
            service.refund(t4.getTransactionId(), 100.00); // Should fail (only 75 available)
            System.out.println("ERROR: Should have thrown exception!");
        } catch (InvalidRefundAmountException e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }
        System.out.println();

        // Test 5: Transaction not found
        System.out.println("--- Test 5: Transaction Not Found ---");
        try {
            service.getTransaction("non-existent-id");
            System.out.println("ERROR: Should have thrown exception!");
        } catch (TransactionNotFoundException e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }
        System.out.println();

        // Test 6: Cannot capture without authorization
        System.out.println("--- Test 6: Cannot Capture Without Authorization ---");
        Transaction t6 = service.createTransaction("user-222", 30.00);
        try {
            service.capture(t6.getTransactionId()); // Should fail - not authorized
            System.out.println("ERROR: Should have thrown exception!");
        } catch (InvalidTransactionStateException e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }
        System.out.println();

        // Test 7: Cannot refund without capture
        System.out.println("--- Test 7: Cannot Refund Without Capture ---");
        Transaction t7 = service.createTransaction("user-333", 40.00);
        service.authorize(t7.getTransactionId());
        try {
            service.refund(t7.getTransactionId(), 20.00); // Should fail - not captured
            System.out.println("ERROR: Should have thrown exception!");
        } catch (InvalidTransactionStateException e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }
        System.out.println();

        System.out.println("=== All Tests Completed Successfully! ===");
    }
}
