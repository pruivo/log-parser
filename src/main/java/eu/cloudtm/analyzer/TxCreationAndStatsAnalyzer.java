package eu.cloudtm.analyzer;

import eu.cloudtm.LogEntry;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

/**
 * @author Pedro Ruivo
 * @since 2.8
 */
public class TxCreationAndStatsAnalyzer implements Analyzer {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss,SSS");
    private final HashMap<String, Transaction> transactions;
    private final ArrayList<Transaction> committed;

    public TxCreationAndStatsAnalyzer() {
        transactions = new HashMap<String, Transaction>();
        committed = new ArrayList<Transaction>();
    }

    private static String prettyPrintTime(long time) {
        if (time == -1) {
            return "N/A";
        }
        return DATE_FORMAT.format(new Date(time));
    }

    @Override
    public void before() {
        transactions.clear();
        committed.clear();
    }

    @Override
    public void after() {
        committed.addAll(transactions.values());
        Collections.sort(committed);
        ArrayList<Transaction> notValid = new ArrayList<Transaction>();
        for (Transaction transaction : committed) {
            System.out.println(transaction);
            if (!transaction.isValid()) {
                notValid.add(transaction);
            }
        }

        System.err.println("##########################################################################");
        System.err.println("##########################################################################");
        System.err.println("############################### NOT VALID ################################");
        System.err.println("##########################################################################");
        System.err.println("##########################################################################");
        for (Transaction transaction : notValid) {
            System.err.println(transaction);
        }
        System.err.println("################################## END ###################################");
    }

    @Override
    public void analyze(LogEntry logEntry) {
        String thread = logEntry.thread();
        EventType type = parse(logEntry.message());
        if (type == null) {
            return; //ignored
        }
        switch (type) {
            case BEGIN:
                if (transactions.containsKey(thread)) {
                    Transaction old = transactions.remove(thread);
                    committed.add(old);
                }
                Transaction newTx = new Transaction(thread);
                newTx.setBegin(logEntry);
                transactions.put(thread, newTx);
                break;
            case COMMIT:
                Transaction tx = transactions.get(thread);
                if (tx == null) {
                    System.err.println("tx does not exist: " + logEntry);
                    break;
                }
                tx.setCommit(logEntry);
                break;
            case GET:
                tx = transactions.get(thread);
                if (tx == null) {
                    System.err.println("tx does not exist: " + logEntry);
                    break;
                }
                tx.setFirstGet(logEntry);
                break;
            case PUT:
                tx = transactions.get(thread);
                if (tx == null) {
                    System.err.println("tx does not exist: " + logEntry);
                    break;
                }
                tx.setFirstPut(logEntry);
                break;
            case CREATE_STAT:
                tx = transactions.get(thread);
                if (tx == null) {
                    System.err.println("tx does not exist: " + logEntry);
                    break;
                }
                tx.setCreateStatsTime(logEntry);
                break;
            case END_STAT:
                tx = transactions.get(thread);
                if (tx == null) {
                    System.err.println("tx does not exist: " + logEntry);
                    break;
                }
                tx.setEndStatsTime(logEntry);
                break;
            default:
                System.err.print("unknonw " + type);

        }
    }

    private EventType parse(String message) {
        for (EventType eventType : EventType.values()) {
            if (eventType.matches(message)) {
                return eventType;
            }
        }
        return null;
    }

    private static enum EventType {
        BEGIN("Begin transaction"),
        COMMIT("Commit transaction"),
        GET("Invoked with command GetKeyValueCommand"),
        PUT("Invoked with command PutKeyValueCommand"),
        CREATE_STAT("Created transaction statistics"),
        END_STAT("Terminating transaction");
        private final String startsWith;

        private EventType(String startsWith) {
            this.startsWith = startsWith;
        }

        public final boolean matches(String message) {
            return message.startsWith(startsWith);
        }
    }

    private class Transaction implements Comparable<Transaction> {
        private final String thread;
        private int beginLine;
        private long begin;
        private int commitLine;
        private long commit;
        private long createStatsTime;
        private long endStatsTime;
        private long firstGet;
        private long firstPut;

        public Transaction(String thread) {
            this.thread = thread;
            begin = commit = createStatsTime = endStatsTime = firstGet = firstPut = -1;
        }

        private void setBegin(LogEntry logEntry) {
            if (this.begin != -1) {
                System.err.println("duplicated begin! " + logEntry);
                return;
            }
            this.begin = logEntry.time();
            this.beginLine = logEntry.lineNumber();
        }

        private void setCommit(LogEntry logEntry) {
            if (this.commit != -1) {
                System.err.println("duplicated commit! " + logEntry);
                return;
            }
            this.commit = logEntry.time();
            this.commitLine = logEntry.lineNumber();
        }

        private void setCreateStatsTime(LogEntry logEntry) {
            if (this.createStatsTime != -1) {
                System.err.println("duplicated create stat time! " + logEntry);
                return;
            }
            this.createStatsTime = logEntry.time();
        }

        private void setEndStatsTime(LogEntry logEntry) {
            if (this.endStatsTime != -1) {
                System.out.println("duplicated end stat time! " + logEntry);
                return;
            }
            this.endStatsTime = logEntry.time();
        }

        private void setFirstGet(LogEntry logEntry) {
            if (this.firstGet != -1) {
                return;
            }
            this.firstGet = logEntry.time();
        }

        private void setFirstPut(LogEntry logEntry) {
            if (this.firstPut != -1) {
                return;
            }
            this.firstPut = logEntry.time();
        }

        @Override
        public String toString() {
            return "Transaction{" +
                    "thread=" + thread +
                    ", begin=" + prettyPrintTime(begin) + "@" + beginLine +
                    ", commit=" + prettyPrintTime(commit) + "@" + commitLine +
                    ", createStatsTime=" + prettyPrintTime(createStatsTime) +
                    ", endStatsTime=" + prettyPrintTime(endStatsTime) +
                    ", firstGet=" + prettyPrintTime(firstGet) +
                    ", firstPut=" + prettyPrintTime(firstPut) +
                    '}';
        }

        @Override
        public int compareTo(Transaction o) {
            if (o == null) {
                return -1;
            }
            Long thisDuration = commit - begin;
            Long otherDuration = o.commit - o.begin;
            return thisDuration.compareTo(otherDuration);
        }

        public final boolean isValid() {
            return begin != -1 && commit != -1 && createStatsTime != -1 && endStatsTime != -1;
        }
    }
}
