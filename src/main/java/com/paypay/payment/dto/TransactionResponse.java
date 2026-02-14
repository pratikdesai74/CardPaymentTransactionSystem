package com.paypay.payment.dto;

import com.paypay.payment.entity.Transaction;
import com.paypay.payment.entity.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private String transactionId;
    private String userId;
    private double amount;
    private double refundedAmount;
    private TransactionStatus status;
    private double refundableAmount;

    public static TransactionResponse fromEntity(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .refundedAmount(transaction.getRefundedAmount())
                .status(transaction.getStatus())
                .refundableAmount(transaction.getRefundableAmount())
                .build();
    }
}
