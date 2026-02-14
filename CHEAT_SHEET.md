# Card Payment Transaction System - Interview Cheat Sheet

## PayPay Card Interview Format

```
Platform:    CodeSignal
Duration:    90-120 minutes
Type:        LLD Machine Coding (not theoretical)
Language:    Java
Focus:       Working code > Perfect architecture
```

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

## 5. Questions to Ask Interviewer (First 5 minutes)

```
□ "Should partial refunds be allowed?"              → Usually yes
□ "Can there be multiple partial refunds?"          → Usually yes
□ "Is REFUNDED a terminal state?"                   → Usually yes
□ "Do I need to handle concurrency?"                → Usually no
□ "In-memory storage is fine?"                      → Usually yes
□ "Should I use BigDecimal for amounts?"            → Mention you would in prod
```

---

## 6. PayPay-Specific Talking Points

**When explaining your design, say this:**

> "I've separated entity, repository, and service to keep responsibilities clear and allow easy extension if we add new payment methods later."

**When asked about trade-offs:**

> "For this implementation, I used double for simplicity. In production, I'd use BigDecimal to avoid floating-point precision issues with money."

**When asked about extensibility:**

> "Adding a new state like VOIDED would require: adding to the enum, adding a void() method to service with state validation. The repository stays unchanged."

---

## 7. Common Follow-ups (Be Ready)

| Question | Answer |
|----------|--------|
| Why not put logic in entity? | Service layer centralizes business rules, easier to test |
| How to add concurrency? | Lock per transactionId, or DB optimistic locking with version |
| Why double not BigDecimal? | Demo simplicity, would use BigDecimal in production |
| How to add new state (VOIDED)? | Add to enum, add method to service, validate transitions |
| How would you add audit logs? | Add AuditService, call before state changes, or use events |

---

## 8. Time Budget (90 min interview)

```
00-10:  Read requirements + ask clarifying questions
10-15:  Explain approach, draw state diagram
15-60:  CODE (45 min - this is the main part!)
        - Enum: 1 min
        - Entity: 3 min
        - Exceptions: 3 min
        - Repository: 3 min
        - Service: 25 min
        - Main/tests: 10 min
60-75:  Add edge case handling + validations
75-90:  Discussion, extensions, Q&A
```

---

## 9. CodeSignal Tips

```
□ Single file may be required - use CodeSignalSolution.java
□ Make sure code compiles - no red squiggles
□ Run your tests before submitting
□ Use clear variable names (interviewer reads your code)
□ Add brief comments for complex logic only
```

---

## 10. Red Flags to AVOID

```
❌ One giant class with everything
❌ No enums - using strings for status
❌ Streams everywhere (keep it simple)
❌ Over-engineering (factories, builders for simple things)
❌ No input validation
❌ Unclear exception messages
❌ Not explaining as you code
```

---

## 11. Green Flags (PayPay LOVES this)

```
✅ Enum-driven state machine
✅ Clean 3-layer separation (entity/repo/service)
✅ Simple if-else checks (not overusing streams)
✅ Custom exceptions with clear messages
✅ Explaining decisions while coding
✅ Asking questions upfront
✅ Testing happy path + edge cases
```

---

## 12. Backup Problem: Rate Limiter

If they ask Rate Limiter instead:

```java
// Fixed Window - simpler
Map<String, WindowData> userWindows = new HashMap<>();

public boolean allowRequest(String userId) {
    long currentWindow = System.currentTimeMillis() / windowSizeMs;
    WindowData data = userWindows.get(userId);

    if (data == null || data.windowId != currentWindow) {
        userWindows.put(userId, new WindowData(currentWindow, 1));
        return true;
    }

    if (data.count < maxRequests) {
        data.count++;
        return true;
    }
    return false; // Rate limited
}
```

---

## 13. Final 30-Min Checklist

```
□ Can write TransactionStatus enum from memory
□ Know the 4 states: CREATED → AUTHORIZED → CAPTURED → REFUNDED
□ Know refund logic (partial stays CAPTURED, full → REFUNDED)
□ Ready to explain repository interface (swap implementations)
□ Have 3-4 clarifying questions ready
□ Practiced typing service methods
□ Know how to add VOIDED state if asked
□ Calm and confident!
```

---

**You've prepared well. Trust your practice!** 🎯
