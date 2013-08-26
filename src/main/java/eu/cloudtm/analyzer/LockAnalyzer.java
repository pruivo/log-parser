package eu.cloudtm.analyzer;

import eu.cloudtm.LogEntry;

import java.util.*;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public class LockAnalyzer implements Analyzer {

    private static final String LOCK_ID_PROPERTY = "lockId";
    private static final String SUCCESS = "SUCCESS";
    private static final String ACQUIRE_SHARED_STRING = "tryAcquireShared";
    private static final String ACQUIRE_EXCLUSIVE_STRING = "tryAcquire";
    private static final String RELEASE_SHARED_STRING = "tryReleaseShared";
    private static final String RELEASE_EXCLUSIVE_STRING = "tryRelease";
    private final Set<String> lockIdsFilter;
    private final Map<String, Transaction> transactionMap;

    public LockAnalyzer() {
        String property = System.getProperty(LOCK_ID_PROPERTY);
        if (property == null) {
            lockIdsFilter = Collections.emptySet();
        } else {
            lockIdsFilter = extractLockIds(property);
        }
        transactionMap = new HashMap<String, Transaction>();
    }

    @Override
    public void before() {
        transactionMap.clear();
    }

    @Override
    public void after() {
        int errors = 0;
        for (Transaction transaction : transactionMap.values()) {
            if (!transaction.check()) {
                errors++;
            }
        }
        System.out.println("Error found: " + errors);
    }

    @Override
    public void analyze(LogEntry logEntry) {
        final String message = logEntry.message();
        if (message.contains(ACQUIRE_EXCLUSIVE_STRING) || message.contains(ACQUIRE_SHARED_STRING)) {
            acquire(parse(message));
        } else if (message.contains(RELEASE_SHARED_STRING) || message.contains(RELEASE_EXCLUSIVE_STRING)) {
            release(parse(message));
        }
    }

    private void release(LockEntry parse) {
        if (parse == null) {
            return;
        }
        if (parse.success && analyzeLock(parse.lockId)) {
            Transaction transaction = transactionMap.get(parse.transaction);
            if (transaction == null) {
                transaction = new Transaction(parse.transaction);
                transactionMap.put(parse.transaction, transaction);
            }
            transaction.release(parse.lockId);
        }
    }

    private void acquire(LockEntry parse) {
        if (parse == null) {
            return;
        }
        if (parse.success && analyzeLock(parse.lockId)) {
            Transaction transaction = transactionMap.get(parse.transaction);
            if (transaction == null) {
                transaction = new Transaction(parse.transaction);
                transactionMap.put(parse.transaction, transaction);
            }
            transaction.acquire(parse.lockId);
        }
    }

    private boolean analyzeLock(String lockId) {
        return lockIdsFilter.isEmpty() || lockIdsFilter.contains(lockId);
    }

    private LockEntry parse(String message) {
        try {
            String[] spaceSplit = message.split(" ");
            String transaction = spaceSplit[0];
            String success = spaceSplit[3];
            String id = message.split("[()]", 3)[1];
            return new LockEntry(transaction, id, success);
        } catch (Exception e) {
            //System.err.println("Exception: " + e + ": " + message);
            //e.printStackTrace();
            return null;
        }
    }

    private Set<String> extractLockIds(String values) {
        return new HashSet<String>(Arrays.asList(values.split(",")));
    }

    private class LockEntry {
        private final String transaction;
        private final String lockId;
        private final boolean success;

        public LockEntry(String transaction, String lockId, String success) {
            this.transaction = transaction;
            this.lockId = lockId;
            this.success = SUCCESS.equals(success);
        }

        @Override
        public String toString() {
            return "LockEntry{" +
                    "transaction='" + transaction + '\'' +
                    ", lockId='" + lockId + '\'' +
                    ", success='" + success + '\'' +
                    '}';
        }
    }

    private final class Transaction {
        private final String transaction;
        private final Set<String> locks;

        public Transaction(String transaction) {
            this.transaction = transaction;
            locks = new HashSet<String>();
        }

        public final void acquire(String lockId) {
            locks.add(lockId);
        }

        public final void release(String lockId) {
            if (!locks.remove(lockId)) {
                System.err.println("Transaction " + transaction + " released twice lock " + lockId);
            }
        }

        public final boolean check() {
            if (!locks.isEmpty()) {
                System.err.println("Transaction " + transaction + " has pending locks: " + locks);
                return false;
            }
            return true;
        }
    }
}
