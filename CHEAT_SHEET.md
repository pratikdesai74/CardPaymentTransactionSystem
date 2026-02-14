# Card Payment Transaction System - Interview Cheat Sheet

---

## 1. State Transitions (MEMORIZE THIS)

```
CREATED ──authorize()──> AUTHORIZED ──capture()──> CAPTURED ──refund()──> REFUNDED
                                                       │
                                                       └── partial refund stays CAPTURED
```

---

## 2. Class Structure (Write in this order)

```
1. TransactionStatus (enum)     - 30 seconds
2. Transaction (entity)         - 2 minutes
3. TransactionRepository (interface) - 1 minute
4. InMemoryTransactionRepository    - 2 minutes
5. Custom Exceptions (3 classes)    - 3 minutes
6. TransactionService              - 15-20 minutes (MAIN FOCUS)
```

---

## 3. Service Method Pattern (All methods follow this)

```java
public void authorize(String transactionId) {
    // 1. Fetch
    Transaction txn = getTransactionOrThrow(transactionId);

    // 2. Validate state
    if (txn.getStatus() != TransactionStatus.CREATED) {
        throw new InvalidTransactionStateException(...);
    }

    // 3. Mutate
    txn.setStatus(TransactionStatus.AUTHORIZED);

    // 4. Save
    repository.save(txn);
}
```

---

## 4. Refund Logic (Tricky Part)

```java
public void refund(String transactionId, double amount) {
    // Validate amount > 0
    // Fetch transaction
    // Validate status == CAPTURED

    double available = txn.getRefundableAmount();  // amount - refundedAmount
    if (amount > available) {
        throw new InvalidRefundAmountException(...);
    }

    txn.setRefundedAmount(txn.getRefundedAmount() + amount);

    // Full refund? Change status
    if (txn.getRefundedAmount() >= txn.getAmount()) {
        txn.setStatus(TransactionStatus.REFUNDED);
    }

    repository.save(txn);
}
```

---

## 5. Questions to Ask (First 5 minutes)

```
□ "Can I do partial refunds?"                    → Yes
□ "Multiple refunds allowed?"                    → Yes
□ "Is REFUNDED terminal?"                        → Yes
□ "Should I handle concurrency?"                 → Usually no
□ "BigDecimal or double for amounts?"            → Mention you'd use BigDecimal in prod
```

---

## 6. Explanation Script (Say this when asked about design)

> "I separated into three layers:
> - **Entity** layer for Transaction and status enum
> - **Repository** interface for storage abstraction - easy to swap implementations
> - **Service** layer for all business logic and state validation
>
> Custom exceptions provide clear error messages for debugging."

---

## 7. Common Follow-ups (Be Ready)

| Question | Answer |
|----------|--------|
| Why not put logic in entity? | Service layer centralizes business rules |
| How to add concurrency? | Lock per transactionId, or DB optimistic locking |
| Why double not BigDecimal? | Demo simplicity, would use BigDecimal in prod |
| How to add new state? | Add to enum, add method to service |

---

## 8. Time Budget (85 min total)

```
00-10: Questions + explain approach
10-20: Draw state diagram, explain layers
20-65: CODE (45 min for implementation)
65-85: Edge cases + discussion
```

---

## 9. Compile & Run Commands

```bash
# Compile
javac -d out src/main/java/com/paypay/payment/**/*.java src/main/java/com/paypay/payment/Main.java

# Run
java -cp out com.paypay.payment.Main
```

---

## 10. Final Checklist Before Interview

```
□ Can write enum from memory
□ Know the 4 states and valid transitions
□ Know refund logic (partial stays CAPTURED)
□ Ready to explain why interface for repository
□ Have clarifying questions ready
□ Practiced typing the service methods
```

---

**You've got this!**
