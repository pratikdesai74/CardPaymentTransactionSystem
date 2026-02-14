# LLD Diagrams - Copy for Whiteboard/Drawing

Use these diagrams during the interview when explaining your design.

---

## 1. STATE MACHINE (Draw First!)

```
                    ┌─────────────────────────────────────────────────┐
                    │           TRANSACTION STATE MACHINE             │
                    └─────────────────────────────────────────────────┘

                              createTransaction()
                                     │
                                     ▼
                          ┌───────────────────┐
                          │                   │
                          │     CREATED       │
                          │                   │
                          └─────────┬─────────┘
                                    │
                                    │ authorize()
                                    ▼
                          ┌───────────────────┐
                          │                   │
                          │   AUTHORIZED      │
                          │                   │
                          └─────────┬─────────┘
                                    │
                                    │ capture()
                                    ▼
                          ┌───────────────────┐
               ┌─────────>│                   │
               │          │    CAPTURED       │<──────────────┐
               │          │                   │               │
               │          └─────────┬─────────┘               │
               │                    │                         │
               │                    │ refund(full)            │ refund(partial)
               │                    ▼                         │
               │          ┌───────────────────┐               │
               │          │                   │               │
               └──────────│    REFUNDED       │───────────────┘
                          │   (terminal)      │
                          └───────────────────┘
```

---

## 2. ARCHITECTURE LAYERS (Draw Second!)

```
    ┌─────────────────────────────────────────────────────────────┐
    │                                                             │
    │                      CLIENT / MAIN                          │
    │                                                             │
    └──────────────────────────────┬──────────────────────────────┘
                                   │
                                   │ calls
                                   ▼
    ┌─────────────────────────────────────────────────────────────┐
    │                     SERVICE LAYER                           │
    │  ┌───────────────────────────────────────────────────────┐  │
    │  │              TransactionService                       │  │
    │  │                                                       │  │
    │  │  • createTransaction(userId, amount) → Transaction    │  │
    │  │  • authorize(transactionId)          → void           │  │
    │  │  • capture(transactionId)            → void           │  │
    │  │  • refund(transactionId, amount)     → void           │  │
    │  │  • getTransaction(transactionId)     → Transaction    │  │
    │  │                                                       │  │
    │  └───────────────────────────────────────────────────────┘  │
    │                           │                                 │
    │                           │ uses                            │
    └───────────────────────────┼─────────────────────────────────┘
                                │
                                ▼
    ┌─────────────────────────────────────────────────────────────┐
    │                    REPOSITORY LAYER                         │
    │                                                             │
    │   ┌─────────────────────────┐                               │
    │   │    <<interface>>        │                               │
    │   │ TransactionRepository   │                               │
    │   │                         │                               │
    │   │  • save(Transaction)    │                               │
    │   │  • findById(String)     │                               │
    │   │  • exists(String)       │                               │
    │   └────────────┬────────────┘                               │
    │                │                                            │
    │                │ implements                                 │
    │                ▼                                            │
    │   ┌─────────────────────────────────────────┐               │
    │   │   InMemoryTransactionRepository         │               │
    │   │                                         │               │
    │   │   Map<String, Transaction> store        │               │
    │   │                                         │               │
    │   └─────────────────────────────────────────┘               │
    │                                                             │
    └─────────────────────────────────────────────────────────────┘
                                │
                                │ stores
                                ▼
    ┌─────────────────────────────────────────────────────────────┐
    │                      ENTITY LAYER                           │
    │                                                             │
    │   ┌─────────────────────────┐   ┌───────────────────────┐   │
    │   │      Transaction        │   │  TransactionStatus    │   │
    │   │                         │   │      <<enum>>         │   │
    │   │  - transactionId        │   │                       │   │
    │   │  - userId               │   │  CREATED              │   │
    │   │  - amount               │──>│  AUTHORIZED           │   │
    │   │  - refundedAmount       │   │  CAPTURED             │   │
    │   │  - status               │   │  REFUNDED             │   │
    │   │                         │   │                       │   │
    │   │  + getRefundableAmount()│   └───────────────────────┘   │
    │   └─────────────────────────┘                               │
    │                                                             │
    └─────────────────────────────────────────────────────────────┘
```

---

## 3. CLASS DIAGRAM (Simplified)

```
┌────────────────────────────────────────────────────────────────────────────┐
│                          CLASS RELATIONSHIPS                                │
└────────────────────────────────────────────────────────────────────────────┘

    ┌──────────────────────┐
    │ TransactionStatus    │
    │     <<enum>>         │
    ├──────────────────────┤
    │ CREATED              │
    │ AUTHORIZED           │
    │ CAPTURED             │
    │ REFUNDED             │
    └──────────┬───────────┘
               │
               │ has-a
               │
    ┌──────────▼───────────┐           ┌──────────────────────────┐
    │    Transaction       │           │  TransactionService      │
    ├──────────────────────┤           ├──────────────────────────┤
    │ - transactionId      │◄──────────│ - repository             │
    │ - userId             │  creates  ├──────────────────────────┤
    │ - amount             │           │ + createTransaction()    │
    │ - refundedAmount     │           │ + authorize()            │
    │ - status             │           │ + capture()              │
    ├──────────────────────┤           │ + refund()               │
    │ + getRefundable()    │           │ + getTransaction()       │
    └──────────────────────┘           └────────────┬─────────────┘
               ▲                                    │
               │                                    │ uses
               │ stores                             │
               │                                    ▼
    ┌──────────┴─────────────────────────────────────────────────┐
    │              TransactionRepository <<interface>>            │
    ├─────────────────────────────────────────────────────────────┤
    │ + save(Transaction)                                         │
    │ + findById(String): Transaction                             │
    │ + exists(String): boolean                                   │
    └─────────────────────────────────────────────────────────────┘
                              ▲
                              │ implements
                              │
    ┌─────────────────────────┴───────────────────────────────────┐
    │              InMemoryTransactionRepository                   │
    ├─────────────────────────────────────────────────────────────┤
    │ - store: Map<String, Transaction>                           │
    ├─────────────────────────────────────────────────────────────┤
    │ + save(Transaction)                                         │
    │ + findById(String): Transaction                             │
    │ + exists(String): boolean                                   │
    └─────────────────────────────────────────────────────────────┘
```

---

## 4. SEQUENCE: HAPPY PATH (Full Lifecycle)

```
┌────────────────────────────────────────────────────────────────────────────┐
│                    SEQUENCE: FULL TRANSACTION LIFECYCLE                     │
└────────────────────────────────────────────────────────────────────────────┘

    Client                Service               Repository
      │                      │                      │
      │  createTransaction   │                      │
      │  (userId, 100.0)     │                      │
      │─────────────────────>│                      │
      │                      │  save(txn)           │
      │                      │─────────────────────>│
      │                      │                      │
      │    Transaction       │                      │
      │    [CREATED]         │                      │
      │<─────────────────────│                      │
      │                      │                      │
      │                      │                      │
      │  authorize(txnId)    │                      │
      │─────────────────────>│                      │
      │                      │  findById            │
      │                      │─────────────────────>│
      │                      │  txn                 │
      │                      │<─────────────────────│
      │                      │                      │
      │                      │  [validate CREATED]  │
      │                      │  [set AUTHORIZED]    │
      │                      │                      │
      │                      │  save(txn)           │
      │                      │─────────────────────>│
      │      void            │                      │
      │<─────────────────────│                      │
      │                      │                      │
      │                      │                      │
      │  capture(txnId)      │                      │
      │─────────────────────>│                      │
      │                      │  [validate AUTHORIZED]
      │                      │  [set CAPTURED]      │
      │                      │  save(txn)           │
      │                      │─────────────────────>│
      │      void            │                      │
      │<─────────────────────│                      │
      │                      │                      │
      │                      │                      │
      │  refund(txnId, 100)  │                      │
      │─────────────────────>│                      │
      │                      │  [validate CAPTURED] │
      │                      │  [check amount <= 100]
      │                      │  [set refundedAmount]│
      │                      │  [set REFUNDED]      │
      │                      │  save(txn)           │
      │                      │─────────────────────>│
      │      void            │                      │
      │<─────────────────────│                      │
      │                      │                      │
```

---

## 5. SEQUENCE: ERROR FLOW

```
┌────────────────────────────────────────────────────────────────────────────┐
│                    SEQUENCE: INVALID STATE TRANSITION                       │
└────────────────────────────────────────────────────────────────────────────┘

    Client                Service               Repository
      │                      │                      │
      │  capture(txnId)      │                      │
      │  [txn is CREATED]    │                      │
      │─────────────────────>│                      │
      │                      │  findById            │
      │                      │─────────────────────>│
      │                      │  txn [CREATED]       │
      │                      │<─────────────────────│
      │                      │                      │
      │                      │  validate status     │
      │                      │  == AUTHORIZED       │
      │                      │        ╳             │
      │                      │      FAIL!           │
      │                      │                      │
      │  InvalidTransaction  │                      │
      │  StateException      │                      │
      │  "Cannot capture in  │                      │
      │   status CREATED"    │                      │
      │<─────────────────────│                      │
      │                      │                      │
```

---

## 6. DATA MODEL

```
┌────────────────────────────────────────────────────────────────────────────┐
│                            TRANSACTION ENTITY                               │
└────────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────┐
    │                      Transaction                             │
    ├─────────────────────────────────────────────────────────────┤
    │                                                             │
    │   transactionId : String     "uuid-1234-5678-abcd"          │
    │                              (immutable, unique)             │
    │                                                             │
    │   userId        : String     "user-123"                     │
    │                              (immutable)                     │
    │                                                             │
    │   amount        : double     100.00                         │
    │                              (immutable, original amount)    │
    │                                                             │
    │   refundedAmount: double     0.00 → 50.00 → 100.00          │
    │                              (mutable, accumulates)          │
    │                                                             │
    │   status        : Enum       CREATED → AUTHORIZED →          │
    │                              CAPTURED → REFUNDED             │
    │                              (mutable, lifecycle state)      │
    │                                                             │
    ├─────────────────────────────────────────────────────────────┤
    │   Derived:                                                  │
    │   refundableAmount = amount - refundedAmount                │
    │                    = 100.00 - 50.00 = 50.00                 │
    └─────────────────────────────────────────────────────────────┘
```

---

## 7. VALIDATION RULES SUMMARY

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         VALIDATION RULES                                    │
├─────────────────────┬──────────────────────────────────────────────────────┤
│ Operation           │ Rules                                                 │
├─────────────────────┼──────────────────────────────────────────────────────┤
│ createTransaction   │ • userId != null && !empty                           │
│                     │ • amount > 0                                          │
├─────────────────────┼──────────────────────────────────────────────────────┤
│ authorize           │ • transaction exists                                  │
│                     │ • status == CREATED                                   │
├─────────────────────┼──────────────────────────────────────────────────────┤
│ capture             │ • transaction exists                                  │
│                     │ • status == AUTHORIZED                                │
├─────────────────────┼──────────────────────────────────────────────────────┤
│ refund              │ • amount > 0                                          │
│                     │ • transaction exists                                  │
│                     │ • status == CAPTURED                                  │
│                     │ • amount <= refundableAmount                          │
├─────────────────────┼──────────────────────────────────────────────────────┤
│ getTransaction      │ • transaction exists                                  │
└─────────────────────┴──────────────────────────────────────────────────────┘
```

---

**Tip**: During the interview, draw the STATE MACHINE first (takes 30 seconds), then explain layers while coding.
