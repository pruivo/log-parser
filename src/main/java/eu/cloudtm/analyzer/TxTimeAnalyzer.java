package eu.cloudtm.analyzer;

import eu.cloudtm.LogEntry;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public class TxTimeAnalyzer implements Analyzer {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance();
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss,SSS");
    private final Map<String, StatsDuration> statsDurations = new HashMap<String, StatsDuration>();

    @Override
    public void before() {
        statsDurations.clear();
    }

    @Override
    public void after() {
        List<StatsDuration> statsDurationList = new ArrayList<StatsDuration>(statsDurations.values());
        Collections.sort(statsDurationList);
        for (StatsDuration statsDuration : statsDurationList) {
            System.out.println(statsDuration);
        }
    }

    @Override
    public void analyze(LogEntry logEntry) {
        long time = logEntry.time();
        String message = logEntry.message();
        if (message == null || message.isEmpty()) {
            return;
        }
        boolean isBegin = isStarting(message);
        String id = getId(message);

        if (isBegin) {
            if (statsDurations.put(id, new StatsDuration(id, time)) != null) {
                System.err.println("Error for id [" + id + "]. This id has started twice");
            }
        } else {
            StatsDuration statsDuration = statsDurations.get(id);
            if (statsDuration == null) {
                System.err.println("Error for id [" + id + "]. This id hasn't started");
                return;
            }
            statsDuration.endTime(time);
        }
    }

    private boolean isStarting(String line) {
        return line.startsWith("begin");
    }

    private String getId(String line) {
        char[] array = line.toCharArray();
        int index = 0;
        while (index < array.length) {
            if (array[index++] == '(') {
                break;
            }
        }
        StringBuilder builder = new StringBuilder();
        while (index < array.length) {
            char c = array[index++];
            if (c == ')') {
                break;
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private class StatsDuration implements Comparable<StatsDuration> {
        private final String id;
        private final long startTime;
        private long endTime;

        private StatsDuration(String id, long startTime) {
            this.id = id;
            this.startTime = startTime;
            this.endTime = -1;
        }

        public long duration() {
            return endTime == -1 ? -1 : endTime - startTime;
        }

        public void endTime(long endTime) {
            if (this.endTime != -1) {
                System.err.println("Error for id [" + id + "]. This transaction statistic was already flushed! first flush @ "
                        + DATE_FORMAT.format(new Date(this.endTime)) + " and second flush @ " +
                        DATE_FORMAT.format(new Date(this.endTime)) + ". time elapsed="
                        + (endTime - this.endTime) + " (msec)");
            }
            this.endTime = endTime;
        }

        @Override
        public int compareTo(StatsDuration o) {
            if (duration() == -1) {
                return 1;
            } else if (o.duration() == -1) {
                return -1;
            }
            return Long.valueOf(duration()).compareTo(o.duration());

        }

        @Override
        public String toString() {
            return "StatsDuration{" +
                    "id='" + id + '\'' +
                    ", startTime=" + DATE_FORMAT.format(new Date(startTime)) +
                    ", endTime=" + DATE_FORMAT.format(new Date(endTime)) +
                    ", duration=" + NUMBER_FORMAT.format(duration()) + " (msec)" +
                    '}';
        }
    }
}
