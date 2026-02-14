package com.paypay.payment.exception;

import com.paypay.payment.entity.TransactionStatus;

public class InvalidTransactionStateException extends RuntimeException {

    public InvalidTransactionStateException(String transactionId,
                                            TransactionStatus currentStatus,
                                            String attemptedAction) {
        super("Cannot " + attemptedAction + " transaction " + transactionId +
              " in status " + currentStatus);
    }
}
