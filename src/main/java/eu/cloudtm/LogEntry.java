package eu.cloudtm;

import static eu.cloudtm.Util.LINE_SEPARATOR;
import static eu.cloudtm.Util.prettyPrintTime;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public class LogEntry {

    private final long time;
    private final String level;
    private final String clazz;
    private final String thread;
    private final int lineNumber;
    private String message;

    public LogEntry(int lineNumber, long time, String level, String clazz, String thread, String message) {
        this.lineNumber = lineNumber;
        this.time = time;
        this.level = level;
        this.clazz = clazz;
        this.thread = thread;
        this.message = message;
    }

    public final long time() {
        return time;
    }

    public final String level() {
        return level;
    }

    public final String clazz() {
        return clazz;
    }

    public final String thread() {
        return thread;
    }

    public final String message() {
        return message;
    }

    public final void addNewLine(String newLine) {
        message = message + LINE_SEPARATOR + newLine;
    }

    public final int lineNumber() {
        return lineNumber;
    }

    public final String prettyPrint() {
        return prettyPrintTime(time) + " " + level + " [" + clazz + "] (" + thread + ") " + message;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "lineNumber=" + lineNumber +
                ", time=" + time +
                ", level='" + level + '\'' +
                ", clazz='" + clazz + '\'' +
                ", thread='" + thread + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogEntry logEntry = (LogEntry) o;

        return time == logEntry.time &&
                clazz.equals(logEntry.clazz) &&
                level.equals(logEntry.level) &&
                message.equals(logEntry.message) &&
                thread.equals(logEntry.thread);

    }

    @Override
    public int hashCode() {
        int result = (int) (time ^ (time >>> 32));
        result = 31 * result + level.hashCode();
        result = 31 * result + clazz.hashCode();
        result = 31 * result + thread.hashCode();
        result = 31 * result + message.hashCode();
        return result;
    }
}
