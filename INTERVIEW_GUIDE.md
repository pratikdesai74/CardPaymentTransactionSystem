# PayPay CodeSignal LLD Interview - Complete Execution Guide

> **Review this 30 minutes before your interview**

---

## Interview Format

```
┌─────────────────────────────────────────────────────────────┐
│  Platform:     CodeSignal                                   │
│  Duration:     90-120 minutes                               │
│  Type:         LLD Machine Coding (not theoretical)         │
│  Language:     Java                                         │
│  Focus:        Working code > Perfect architecture          │
│  Storage:      In-memory HashMap (no DB required)           │
│  Concurrency:  Not required (single-threaded)               │
└─────────────────────────────────────────────────────────────┘
```

---

## Pre-Interview Checklist

```
□ Read this guide
□ Can write TransactionStatus enum from memory
□ Know state transitions: CREATED → AUTHORIZED → CAPTURED → REFUNDED
□ Know refund logic (partial stays CAPTURED, full → REFUNDED)
□ Have clarifying questions ready
□ Calm and confident
```

---

# PHASE 1: READ & CLARIFY (0-10 min)

## Step 1: Read Problem (3 min)
- Don't rush
- Note down entities mentioned
- Identify operations required

## Step 2: Ask Clarifying Questions (5 min)

**Say this out loud:**

> "Before I start, I have a few clarifying questions:"

```
1. "Should partial refunds be allowed?"
2. "Can a transaction be refunded multiple times?"
3. "Is REFUNDED a terminal state - no more operations after?"
4. "Do I need to handle concurrent requests?"
5. "Is in-memory storage acceptable?"
6. "Should I focus on core logic first, then add validations?"
```

## Step 3: Confirm Understanding (2 min)

**Say:**

> "So to summarize:
> - Transaction goes through: CREATED → AUTHORIZED → CAPTURED → REFUNDED
> - Partial refunds allowed, multiple times
> - Once fully refunded, no more operations
> - In-memory storage is fine
> - Single-threaded
>
> Does that sound right?"

---

# PHASE 2: EXPLAIN APPROACH (10-15 min)

## Step 4: Explain Design (3 min)

**Say:**

> "I'll structure this in 3 layers:
>
> 1. **ENTITY layer** - Transaction class with status enum
> 2. **REPOSITORY layer** - Interface + in-memory implementation
> 3. **SERVICE layer** - All business logic and state validation
>
> This separation keeps responsibilities clear and makes it easy to extend."

## Step 5: Draw State Diagram (2 min)

**Type or draw:**

```
CREATED ──authorize()──> AUTHORIZED ──capture()──> CAPTURED ──refund()──> REFUNDED
                                                       │
                                                       └── partial refund stays CAPTURED
```

**Say:**

> "Invalid transitions will throw exceptions. For example, you can't capture a CREATED transaction - it must be authorized first."

---

# PHASE 3: CODE (15-60 min) ⭐ MAIN PART

## Coding Order (Memorize This!)

```
┌────────────────────────────────────────────────────────────┐
│  ORDER          │  COMPONENT                    │  TIME    │
├────────────────────────────────────────────────────────────┤
│  1              │  TransactionStatus (enum)     │  1 min   │
│  2              │  Transaction (entity)         │  3 min   │
│  3              │  Exceptions (3 classes)       │  3 min   │
│  4              │  TransactionRepository (intf) │  1 min   │
│  5              │  InMemoryTransactionRepository│  2 min   │
│  6              │  TransactionService           │  15 min  │
│  7              │  Main with tests              │  5 min   │
├────────────────────────────────────────────────────────────┤
│  TOTAL                                          │  ~30 min │
└────────────────────────────────────────────────────────────┘
```

---

## Step 6: TransactionStatus Enum (1 min)

**Say:** *"Starting with the enum for type-safe status management."*

```java
enum TransactionStatus {
    CREATED,
    AUTHORIZED,
    CAPTURED,
    REFUNDED
}
```

---

## Step 7: Transaction Entity (3 min)

**Say:** *"Transaction ID, userId, and amount are immutable. Only status and refundedAmount change."*

```java
class Transaction {
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

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getUserId() { return userId; }
    public double getAmount() { return amount; }
    public double getRefundedAmount() { return refundedAmount; }
    public TransactionStatus getStatus() { return status; }

    // Setters (only for mutable fields)
    public void setRefundedAmount(double refundedAmount) {
        this.refundedAmount = refundedAmount;
    }
    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    // Helper method
    public double getRefundableAmount() {
        return amount - refundedAmount;
    }

    @Override
    public String toString() {
        return "Transaction{id='" + transactionId + "', status=" + status +
               ", amount=" + amount + ", refunded=" + refundedAmount + "}";
    }
}
```

---

## Step 8: Custom Exceptions (3 min)

**Say:** *"Custom exceptions with clear messages for debugging."*

```java
class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String transactionId) {
        super("Transaction not found: " + transactionId);
    }
}

class InvalidTransactionStateException extends RuntimeException {
    public InvalidTransactionStateException(String transactionId,
                                            TransactionStatus currentStatus,
                                            String attemptedAction) {
        super("Cannot " + attemptedAction + " transaction " + transactionId +
              " in status " + currentStatus);
    }
}

class InvalidRefundAmountException extends RuntimeException {
    public InvalidRefundAmountException(String transactionId,
                                        double requested,
                                        double available) {
        super("Cannot refund " + requested + " for transaction " + transactionId +
              ". Available: " + available);
    }
}
```

---

## Step 9: TransactionRepository Interface (1 min)

**Say:** *"Interface allows swapping implementations - HashMap now, database later."*

```java
interface TransactionRepository {
    void save(Transaction transaction);
    Transaction findById(String transactionId);
}
```

---

## Step 10: InMemoryTransactionRepository (2 min)

**Say:** *"Simple HashMap implementation. O(1) lookups."*

```java
class InMemoryTransactionRepository implements TransactionRepository {
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
```

---

## Step 11: TransactionService - Constructor & Helper (2 min)

**Say:** *"Constructor injection for dependency inversion. Helper method to avoid repetition."*

```java
class TransactionService {
    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    private Transaction getTransactionOrThrow(String transactionId) {
        Transaction transaction = repository.findById(transactionId);
        if (transaction == null) {
            throw new TransactionNotFoundException(transactionId);
        }
        return transaction;
    }

    // Methods below...
}
```

---

## Step 12: createTransaction (3 min)

**Say:** *"Validate inputs, generate unique ID, create with CREATED status, save, return."*

```java
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
```

---

## Step 13: authorize (3 min)

**Say:** *"Fetch, validate state is CREATED, transition to AUTHORIZED, save."*

```java
public void authorize(String transactionId) {
    Transaction transaction = getTransactionOrThrow(transactionId);

    if (transaction.getStatus() != TransactionStatus.CREATED) {
        throw new InvalidTransactionStateException(
            transactionId, transaction.getStatus(), "authorize");
    }

    transaction.setStatus(TransactionStatus.AUTHORIZED);
    repository.save(transaction);
}
```

---

## Step 14: capture (3 min)

**Say:** *"Same pattern - must be AUTHORIZED to capture."*

```java
public void capture(String transactionId) {
    Transaction transaction = getTransactionOrThrow(transactionId);

    if (transaction.getStatus() != TransactionStatus.AUTHORIZED) {
        throw new InvalidTransactionStateException(
            transactionId, transaction.getStatus(), "capture");
    }

    transaction.setStatus(TransactionStatus.CAPTURED);
    repository.save(transaction);
}
```

---

## Step 15: refund ⭐ MOST IMPORTANT (5 min)

**Say:** *"This is the tricky one. Validate amount, check we're CAPTURED, check available balance, accumulate refunded amount, only transition to REFUNDED when fully refunded."*

```java
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

    // Only mark REFUNDED when fully refunded
    if (newRefundedAmount >= transaction.getAmount()) {
        transaction.setStatus(TransactionStatus.REFUNDED);
    }

    repository.save(transaction);
}
```

---

## Step 16: getTransaction (1 min)

```java
public Transaction getTransaction(String transactionId) {
    return getTransactionOrThrow(transactionId);
}
```

---

## Step 17: Main with Tests (5 min)

**Say:** *"Let me verify the happy path and a couple of error cases."*

```java
public static void main(String[] args) {
    TransactionRepository repository = new InMemoryTransactionRepository();
    TransactionService service = new TransactionService(repository);

    System.out.println("=== Card Payment Transaction System ===\n");

    // Test 1: Happy path (MUST HAVE)
    System.out.println("--- Test 1: Full Lifecycle ---");
    Transaction t1 = service.createTransaction("user-123", 100.0);
    System.out.println("Created: " + t1);

    service.authorize(t1.getTransactionId());
    System.out.println("Authorized: " + service.getTransaction(t1.getTransactionId()));

    service.capture(t1.getTransactionId());
    System.out.println("Captured: " + service.getTransaction(t1.getTransactionId()));

    service.refund(t1.getTransactionId(), 100.0);
    System.out.println("Refunded: " + service.getTransaction(t1.getTransactionId()));

    // Test 2: Invalid state transition (GOOD TO HAVE)
    System.out.println("\n--- Test 2: Invalid State ---");
    try {
        Transaction t2 = service.createTransaction("user-456", 50.0);
        service.capture(t2.getTransactionId());  // Should fail - not authorized
    } catch (InvalidTransactionStateException e) {
        System.out.println("Caught: " + e.getMessage());
    }

    // Test 3: Partial refund (GOOD TO HAVE)
    System.out.println("\n--- Test 3: Partial Refund ---");
    Transaction t3 = service.createTransaction("user-789", 200.0);
    service.authorize(t3.getTransactionId());
    service.capture(t3.getTransactionId());
    service.refund(t3.getTransactionId(), 50.0);
    System.out.println("After partial refund: " + service.getTransaction(t3.getTransactionId()));
    // Status should still be CAPTURED

    System.out.println("\n=== All Tests Passed! ===");
}
```

---

# PHASE 4: VALIDATE & POLISH (60-75 min)

## Step 18: Run Your Code
- Make sure it compiles (no red squiggles)
- Check output matches expectations
- Fix any typos

## Step 19: Add Missing Edge Cases (if time permits)
- Null checks done ✓
- Empty string checks done ✓
- Negative amount checks done ✓
- Refund exceeds available done ✓

---

# PHASE 5: DISCUSSION (75-90 min)

## Step 20: Common Follow-up Questions

| Question | Your Answer |
|----------|-------------|
| **"Why not put logic in entity?"** | "Service layer centralizes business rules, easier to test and modify" |
| **"How to add concurrency?"** | "Add per-transaction locking with ConcurrentHashMap, or use optimistic locking with version field in DB" |
| **"Why double not BigDecimal?"** | "Simplicity for demo. Production should use BigDecimal to avoid floating-point precision issues with money" |
| **"How to add new state (VOIDED)?"** | "Add to enum, add void() method to service with state validation. Repository stays unchanged" |
| **"How would you add audit logs?"** | "Add AuditService, call before each state change. Or use event sourcing pattern" |
| **"How to persist to DB?"** | "Implement TransactionRepository with JPA/JDBC. Service layer stays unchanged - that's the benefit of the interface" |

---

# QUICK REFERENCE CARD

```
┌─────────────────────────────────────────────────────────────────────┐
│                    STATE TRANSITIONS                                 │
├─────────────────────────────────────────────────────────────────────┤
│  CREATED ──authorize()──> AUTHORIZED ──capture()──> CAPTURED        │
│                                                         │           │
│                                          refund() ──────┤           │
│                                                         │           │
│                              partial: stays CAPTURED <──┤           │
│                              full: ──> REFUNDED <───────┘           │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    SERVICE METHOD PATTERN                            │
├─────────────────────────────────────────────────────────────────────┤
│  1. Fetch:    Transaction txn = getTransactionOrThrow(id);          │
│  2. Validate: if (txn.getStatus() != EXPECTED) throw exception;     │
│  3. Mutate:   txn.setStatus(NEW_STATUS);                            │
│  4. Save:     repository.save(txn);                                 │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    TIME BUDGET (90 min)                              │
├─────────────────────────────────────────────────────────────────────┤
│  00-10:  Read + clarify questions                                   │
│  10-15:  Explain approach + draw state diagram                      │
│  15-60:  CODE (this is the main part!)                              │
│  60-75:  Polish + edge cases                                        │
│  75-90:  Discussion + Q&A                                           │
└─────────────────────────────────────────────────────────────────────┘
```

---

# WHAT TO AVOID ❌

```
❌ One giant class with everything
❌ No enums - using strings for status
❌ Streams everywhere (keep it simple)
❌ Over-engineering (factories, builders)
❌ No input validation
❌ Unclear exception messages
❌ Not explaining as you code
❌ Spending too long on tests (max 5-10 min)
```

# WHAT PAYPAY LOVES ✅

```
✅ Enum-driven state machine
✅ Clean 3-layer separation (entity/repo/service)
✅ Simple if-else checks
✅ Custom exceptions with clear messages
✅ Explaining decisions while coding
✅ Asking questions upfront
✅ Testing happy path + 2-3 edge cases
```

---

# IMPORTS (Add at top of file)

```java
import java.util.*;
```

---

# IF THEY ASK RATE LIMITER INSTEAD

```java
class FixedWindowRateLimiter {
    private final int maxRequests;
    private final long windowSizeMs;
    private final Map<String, WindowData> userWindows = new HashMap<>();

    public FixedWindowRateLimiter(int maxRequests, long windowSizeMs) {
        this.maxRequests = maxRequests;
        this.windowSizeMs = windowSizeMs;
    }

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
        return false;  // Rate limited
    }

    private static class WindowData {
        long windowId;
        int count;
        WindowData(long windowId, int count) {
            this.windowId = windowId;
            this.count = count;
        }
    }
}
```

---

**You've prepared well. Trust your practice!** 🎯

**Good luck with PayPay!**
