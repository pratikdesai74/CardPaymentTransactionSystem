package com.paypay.payment.exception;

public class InvalidRefundAmountException extends RuntimeException {

    public InvalidRefundAmountException(String transactionId,
                                        double requestedAmount,
                                        double availableAmount) {
        super("Cannot refund " + requestedAmount + " for transaction " + transactionId +
              ". Available for refund: " + availableAmount);
    }
}
