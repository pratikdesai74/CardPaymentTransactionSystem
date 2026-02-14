package com.paypay.payment;

import java.util.*;

/**
 * BACKUP PROBLEM: Rate Limiter
 * ============================
 * Fixed Window Rate Limiter implementation.
 *
 * Use case: Limit API calls per user to N requests per time window.
 */
public class RateLimiter {

    // ==================== FIXED WINDOW RATE LIMITER ====================

    static class FixedWindowRateLimiter {
        private final int maxRequests;
        private final long windowSizeMs;
        private final Map<String, WindowData> userWindows = new HashMap<>();

        public FixedWindowRateLimiter(int maxRequests, long windowSizeMs) {
            this.maxRequests = maxRequests;
            this.windowSizeMs = windowSizeMs;
        }

        public boolean allowRequest(String userId) {
            long currentTime = System.currentTimeMillis();
            long currentWindow = currentTime / windowSizeMs;

            WindowData windowData = userWindows.get(userId);

            // New user or new window
            if (windowData == null || windowData.windowId != currentWindow) {
                userWindows.put(userId, new WindowData(currentWindow, 1));
                return true;
            }

            // Same window - check limit
            if (windowData.count < maxRequests) {
                windowData.count++;
                return true;
            }

            return false; // Rate limited
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

    // ==================== SLIDING WINDOW LOG RATE LIMITER ====================

    static class SlidingWindowRateLimiter {
        private final int maxRequests;
        private final long windowSizeMs;
        private final Map<String, LinkedList<Long>> userRequestLogs = new HashMap<>();

        public SlidingWindowRateLimiter(int maxRequests, long windowSizeMs) {
            this.maxRequests = maxRequests;
            this.windowSizeMs = windowSizeMs;
        }

        public boolean allowRequest(String userId) {
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - windowSizeMs;

            LinkedList<Long> requestLog = userRequestLogs.computeIfAbsent(
                userId, k -> new LinkedList<>());

            // Remove expired timestamps
            while (!requestLog.isEmpty() && requestLog.peekFirst() < windowStart) {
                requestLog.pollFirst();
            }

            // Check limit
            if (requestLog.size() < maxRequests) {
                requestLog.addLast(currentTime);
                return true;
            }

            return false; // Rate limited
        }
    }

    // ==================== MAIN (TEST) ====================

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Rate Limiter Demo ===\n");

        // Test Fixed Window: 3 requests per second
        System.out.println("--- Fixed Window (3 req/sec) ---");
        FixedWindowRateLimiter fixedLimiter = new FixedWindowRateLimiter(3, 1000);

        for (int i = 1; i <= 5; i++) {
            boolean allowed = fixedLimiter.allowRequest("user-1");
            System.out.println("Request " + i + ": " + (allowed ? "ALLOWED" : "BLOCKED"));
        }

        System.out.println("\nWaiting 1 second...");
        Thread.sleep(1100);

        for (int i = 6; i <= 8; i++) {
            boolean allowed = fixedLimiter.allowRequest("user-1");
            System.out.println("Request " + i + ": " + (allowed ? "ALLOWED" : "BLOCKED"));
        }

        // Test Sliding Window
        System.out.println("\n--- Sliding Window (3 req/sec) ---");
        SlidingWindowRateLimiter slidingLimiter = new SlidingWindowRateLimiter(3, 1000);

        for (int i = 1; i <= 5; i++) {
            boolean allowed = slidingLimiter.allowRequest("user-2");
            System.out.println("Request " + i + ": " + (allowed ? "ALLOWED" : "BLOCKED"));
            Thread.sleep(200); // Space out requests
        }

        System.out.println("\n=== Rate Limiter Tests Complete ===");
    }
}
