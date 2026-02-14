package com.paypay.payment.service;

import com.paypay.payment.entity.Transaction;
import com.paypay.payment.entity.TransactionStatus;
import com.paypay.payment.exception.InvalidRefundAmountException;
import com.paypay.payment.exception.InvalidTransactionStateException;
import com.paypay.payment.exception.TransactionNotFoundException;
import com.paypay.payment.repository.InMemoryTransactionRepository;
import com.paypay.payment.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for TransactionService.
 * Tests cover all state transitions, validations, and edge cases.
 */
class TransactionServiceTest {

    private TransactionRepository repository;
    private TransactionService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        service = new TransactionService(repository);
    }

    // ==================== CREATE TRANSACTION TESTS ====================

    @Nested
    @DisplayName("createTransaction()")
    class CreateTransactionTests {

        @Test
        @DisplayName("should create transaction with CREATED status")
        void shouldCreateTransactionWithCreatedStatus() {
            Transaction transaction = service.createTransaction("user-123", 100.0);

            assertThat(transaction).isNotNull();
            assertThat(transaction.getTransactionId()).isNotNull().isNotEmpty();
            assertThat(transaction.getUserId()).isEqualTo("user-123");
            assertThat(transaction.getAmount()).isEqualTo(100.0);
            assertThat(transaction.getRefundedAmount()).isEqualTo(0.0);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.CREATED);
        }

        @Test
        @DisplayName("should generate unique transaction IDs")
        void shouldGenerateUniqueTransactionIds() {
            Transaction t1 = service.createTransaction("user-1", 100.0);
            Transaction t2 = service.createTransaction("user-2", 200.0);

            assertThat(t1.getTransactionId()).isNotEqualTo(t2.getTransactionId());
        }

        @Test
        @DisplayName("should throw exception for null userId")
        void shouldThrowExceptionForNullUserId() {
            assertThatThrownBy(() -> service.createTransaction(null, 100.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("should throw exception for empty userId")
        void shouldThrowExceptionForEmptyUserId() {
            assertThatThrownBy(() -> service.createTransaction("", 100.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("should throw exception for zero amount")
        void shouldThrowExceptionForZeroAmount() {
            assertThatThrownBy(() -> service.createTransaction("user-123", 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should throw exception for negative amount")
        void shouldThrowExceptionForNegativeAmount() {
            assertThatThrownBy(() -> service.createTransaction("user-123", -50.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount");
        }
    }

    // ==================== AUTHORIZE TESTS ====================

    @Nested
    @DisplayName("authorize()")
    class AuthorizeTests {

        @Test
        @DisplayName("should authorize CREATED transaction")
        void shouldAuthorizeCreatedTransaction() {
            Transaction transaction = service.createTransaction("user-123", 100.0);

            service.authorize(transaction.getTransactionId());

            Transaction updated = service.getTransaction(transaction.getTransactionId());
            assertThat(updated.getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);
        }

        @Test
        @DisplayName("should throw exception for non-existent transaction")
        void shouldThrowExceptionForNonExistentTransaction() {
            assertThatThrownBy(() -> service.authorize("non-existent-id"))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessageContaining("non-existent-id");
        }

        @Test
        @DisplayName("should throw exception when authorizing AUTHORIZED transaction")
        void shouldThrowExceptionWhenAuthorizingAuthorizedTransaction() {
            Transaction transaction = service.createTransaction("user-123", 100.0);
            service.authorize(transaction.getTransactionId());

            assertThatThrownBy(() -> service.authorize(transaction.getTransactionId()))
                    .isInstanceOf(InvalidTransactionStateException.class)
                    .hasMessageContaining("authorize")
                    .hasMessageContaining("AUTHORIZED");
        }

        @Test
        @DisplayName("should throw exception when authorizing CAPTURED transaction")
        void shouldThrowExceptionWhenAuthorizingCapturedTransaction() {
            Transaction transaction = service.createTransaction("user-123", 100.0);
            service.authorize(transaction.getTransactionId());
            service.capture(transaction.getTransactionId());

            assertThatThrownBy(() -> service.authorize(transaction.getTransactionId()))
                    .isInstanceOf(InvalidTransactionStateException.class)
                    .hasMessageContaining("CAPTURED");
        }
    }

    // ==================== CAPTURE TESTS ====================

    @Nested
    @DisplayName("capture()")
    class CaptureTests {

        @Test
        @DisplayName("should capture AUTHORIZED transaction")
        void shouldCaptureAuthorizedTransaction() {
            Transaction transaction = service.createTransaction("user-123", 100.0);
            service.authorize(transaction.getTransactionId());

            service.capture(transaction.getTransactionId());

            Transaction updated = service.getTransaction(transaction.getTransactionId());
            assertThat(updated.getStatus()).isEqualTo(TransactionStatus.CAPTURED);
        }

        @Test
        @DisplayName("should throw exception when capturing CREATED transaction")
        void shouldThrowExceptionWhenCapturingCreatedTransaction() {
            Transaction transaction = service.createTransaction("user-123", 100.0);

            assertThatThrownBy(() -> service.capture(transaction.getTransactionId()))
                    .isInstanceOf(InvalidTransactionStateException.class)
                    .hasMessageContaining("capture")
                    .hasMessageContaining("CREATED");
        }

        @Test
        @DisplayName("should throw exception when capturing already CAPTURED transaction")
        void shouldThrowExceptionWhenCapturingCapturedTransaction() {
            Transaction transaction = service.createTransaction("user-123", 100.0);
            service.authorize(transaction.getTransactionId());
            service.capture(transaction.getTransactionId());

            assertThatThrownBy(() -> service.capture(transaction.getTransactionId()))
                    .isInstanceOf(InvalidTransactionStateException.class)
                    .hasMessageContaining("CAPTURED");
        }

        @Test
        @DisplayName("should throw exception for non-existent transaction")
        void shouldThrowExceptionForNonExistentTransaction() {
            assertThatThrownBy(() -> service.capture("non-existent-id"))
                    .isInstanceOf(TransactionNotFoundException.class);
        }
    }

    // ==================== REFUND TESTS ====================

    @Nested
    @DisplayName("refund()")
    class RefundTests {

        private String capturedTransactionId;

        @BeforeEach
        void setUpCapturedTransaction() {
            Transaction transaction = service.createTransaction("user-123", 100.0);
            service.authorize(transaction.getTransactionId());
            service.capture(transaction.getTransactionId());
            capturedTransactionId = transaction.getTransactionId();
        }

        @Test
        @DisplayName("should refund full amount and set REFUNDED status")
        void shouldRefundFullAmountAndSetRefundedStatus() {
            service.refund(capturedTransactionId, 100.0);

            Transaction updated = service.getTransaction(capturedTransactionId);
            assertThat(updated.getRefundedAmount()).isEqualTo(100.0);
            assertThat(updated.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        }

        @Test
        @DisplayName("should allow partial refund and keep CAPTURED status")
        void shouldAllowPartialRefundAndKeepCapturedStatus() {
            service.refund(capturedTransactionId, 30.0);

            Transaction updated = service.getTransaction(capturedTransactionId);
            assertThat(updated.getRefundedAmount()).isEqualTo(30.0);
            assertThat(updated.getStatus()).isEqualTo(TransactionStatus.CAPTURED);
            assertThat(updated.getRefundableAmount()).isEqualTo(70.0);
        }

        @Test
        @DisplayName("should allow multiple partial refunds")
        void shouldAllowMultiplePartialRefunds() {
            service.refund(capturedTransactionId, 30.0);
            service.refund(capturedTransactionId, 40.0);

            Transaction updated = service.getTransaction(capturedTransactionId);
            assertThat(updated.getRefundedAmount()).isEqualTo(70.0);
            assertThat(updated.getStatus()).isEqualTo(TransactionStatus.CAPTURED);
        }

        @Test
        @DisplayName("should transition to REFUNDED after multiple refunds totaling full amount")
        void shouldTransitionToRefundedAfterFullRefund() {
            service.refund(capturedTransactionId, 30.0);
            service.refund(capturedTransactionId, 40.0);
            service.refund(capturedTransactionId, 30.0);

            Transaction updated = service.getTransaction(capturedTransactionId);
            assertThat(updated.getRefundedAmount()).isEqualTo(100.0);
            assertThat(updated.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        }

        @Test
        @DisplayName("should throw exception when refund exceeds available amount")
        void shouldThrowExceptionWhenRefundExceedsAvailableAmount() {
            assertThatThrownBy(() -> service.refund(capturedTransactionId, 150.0))
                    .isInstanceOf(InvalidRefundAmountException.class)
                    .hasMessageContaining("150.0")
                    .hasMessageContaining("100.0");
        }

        @Test
        @DisplayName("should throw exception when refund exceeds remaining amount after partial")
        void shouldThrowExceptionWhenRefundExceedsRemainingAmount() {
            service.refund(capturedTransactionId, 60.0);

            assertThatThrownBy(() -> service.refund(capturedTransactionId, 50.0))
                    .isInstanceOf(InvalidRefundAmountException.class)
                    .hasMessageContaining("50.0")
                    .hasMessageContaining("40.0");
        }

        @Test
        @DisplayName("should throw exception for zero refund amount")
        void shouldThrowExceptionForZeroRefundAmount() {
            assertThatThrownBy(() -> service.refund(capturedTransactionId, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should throw exception for negative refund amount")
        void shouldThrowExceptionForNegativeRefundAmount() {
            assertThatThrownBy(() -> service.refund(capturedTransactionId, -10.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw exception when refunding CREATED transaction")
        void shouldThrowExceptionWhenRefundingCreatedTransaction() {
            Transaction created = service.createTransaction("user-456", 50.0);

            assertThatThrownBy(() -> service.refund(created.getTransactionId(), 25.0))
                    .isInstanceOf(InvalidTransactionStateException.class)
                    .hasMessageContaining("CREATED");
        }

        @Test
        @DisplayName("should throw exception when refunding AUTHORIZED transaction")
        void shouldThrowExceptionWhenRefundingAuthorizedTransaction() {
            Transaction transaction = service.createTransaction("user-456", 50.0);
            service.authorize(transaction.getTransactionId());

            assertThatThrownBy(() -> service.refund(transaction.getTransactionId(), 25.0))
                    .isInstanceOf(InvalidTransactionStateException.class)
                    .hasMessageContaining("AUTHORIZED");
        }

        @Test
        @DisplayName("should throw exception when refunding REFUNDED transaction")
        void shouldThrowExceptionWhenRefundingRefundedTransaction() {
            service.refund(capturedTransactionId, 100.0); // Fully refunded

            assertThatThrownBy(() -> service.refund(capturedTransactionId, 10.0))
                    .isInstanceOf(InvalidTransactionStateException.class)
                    .hasMessageContaining("REFUNDED");
        }
    }

    // ==================== GET TRANSACTION TESTS ====================

    @Nested
    @DisplayName("getTransaction()")
    class GetTransactionTests {

        @Test
        @DisplayName("should return transaction by ID")
        void shouldReturnTransactionById() {
            Transaction created = service.createTransaction("user-123", 100.0);

            Transaction retrieved = service.getTransaction(created.getTransactionId());

            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getTransactionId()).isEqualTo(created.getTransactionId());
            assertThat(retrieved.getUserId()).isEqualTo("user-123");
            assertThat(retrieved.getAmount()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("should throw exception for non-existent transaction")
        void shouldThrowExceptionForNonExistentTransaction() {
            assertThatThrownBy(() -> service.getTransaction("non-existent-id"))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessageContaining("non-existent-id");
        }
    }

    // ==================== FULL LIFECYCLE TESTS ====================

    @Nested
    @DisplayName("Full Lifecycle")
    class FullLifecycleTests {

        @Test
        @DisplayName("should complete full happy path: CREATE -> AUTHORIZE -> CAPTURE -> REFUND")
        void shouldCompleteFullHappyPath() {
            // Create
            Transaction transaction = service.createTransaction("user-123", 100.0);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.CREATED);

            // Authorize
            service.authorize(transaction.getTransactionId());
            assertThat(service.getTransaction(transaction.getTransactionId()).getStatus())
                    .isEqualTo(TransactionStatus.AUTHORIZED);

            // Capture
            service.capture(transaction.getTransactionId());
            assertThat(service.getTransaction(transaction.getTransactionId()).getStatus())
                    .isEqualTo(TransactionStatus.CAPTURED);

            // Refund
            service.refund(transaction.getTransactionId(), 100.0);
            Transaction finalState = service.getTransaction(transaction.getTransactionId());
            assertThat(finalState.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
            assertThat(finalState.getRefundedAmount()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("should handle multiple partial refunds correctly")
        void shouldHandleMultiplePartialRefundsCorrectly() {
            Transaction transaction = service.createTransaction("user-123", 200.0);
            service.authorize(transaction.getTransactionId());
            service.capture(transaction.getTransactionId());

            // First partial refund
            service.refund(transaction.getTransactionId(), 50.0);
            Transaction t1 = service.getTransaction(transaction.getTransactionId());
            assertThat(t1.getRefundedAmount()).isEqualTo(50.0);
            assertThat(t1.getRefundableAmount()).isEqualTo(150.0);
            assertThat(t1.getStatus()).isEqualTo(TransactionStatus.CAPTURED);

            // Second partial refund
            service.refund(transaction.getTransactionId(), 100.0);
            Transaction t2 = service.getTransaction(transaction.getTransactionId());
            assertThat(t2.getRefundedAmount()).isEqualTo(150.0);
            assertThat(t2.getRefundableAmount()).isEqualTo(50.0);
            assertThat(t2.getStatus()).isEqualTo(TransactionStatus.CAPTURED);

            // Final refund - completes the refund
            service.refund(transaction.getTransactionId(), 50.0);
            Transaction t3 = service.getTransaction(transaction.getTransactionId());
            assertThat(t3.getRefundedAmount()).isEqualTo(200.0);
            assertThat(t3.getRefundableAmount()).isEqualTo(0.0);
            assertThat(t3.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        }
    }
}
