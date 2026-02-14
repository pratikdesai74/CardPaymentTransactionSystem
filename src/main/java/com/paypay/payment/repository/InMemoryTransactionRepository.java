package com.paypay.payment.repository;

import com.paypay.payment.entity.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementation of TransactionRepository.
 * Uses a HashMap for O(1) lookup by transactionId.
 */
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<String, Transaction> store = new HashMap<>();

    @Override
    public void save(Transaction transaction) {
        store.put(transaction.getTransactionId(), transaction);
    }

    @Override
    public Transaction findById(String transactionId) {
        return store.get(transactionId);
    }

    @Override
    public boolean exists(String transactionId) {
        return store.containsKey(transactionId);
    }
}
