package eu.cloudtm.parser;

import eu.cloudtm.LogEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.NoSuchElementException;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public class ISPNTestSuiteParser implements Parser {

    private static final DateFormat TIMESTAMP_PARSER = new SimpleDateFormat("HH:mm:ss,SSS");
    private static final DateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd");
    private static final String FORMAT_PROPERTY = "ispn.logFormat";
    private final LogFormat logFormat;

    public ISPNTestSuiteParser() {
        logFormat = LogFormat.fromString(System.getProperty(FORMAT_PROPERTY));
    }

    @Override
    public final LogIterator parse(InputStream inputStream) throws Exception {
        return new LogEntryIterator(inputStream);
    }

    /**
     * @return {@code true} if the line belongs to the log entry (i.e. it is not a new log entry), {@code false} otherwise
     */
    private boolean parseLine(LogEntry logEntry, String line) {
        if (line == null || line.isEmpty()) {
            return true;
        }
        if (!tryParse(new LineState(line.toCharArray()))) {
            logEntry.addNewLine(line);
            return true;
        }
        return false;
    }

    private LogEntry parseLine(int lineNumber, String line) throws Exception {
        if (line == null || line.isEmpty() || Character.isWhitespace(line.charAt(0))) {
            return null;
        }
        LineState state = new LineState(line.toCharArray());
        if (!tryParse(state)) {
            return null;
        }
        long time = 0;
        switch (logFormat) {
            case FORMAT_1:
                //format-new: date{yyyy-mm-dd} time{h:m:s,S} level (thread) [class] message
                time = parseDate(state);
            case FORMAT_3:
                //format-3: time{h:m:s,S} level (thread) [class] message
                time += parseTime(state);
                String level = parseLevel(state);
                String thread = parseThread(state);
                String clazz = parseClass(state);
                return new LogEntry(lineNumber, time, level, clazz, thread, parseMessage(state));
            case FORMAT_2:
                //format-older: date{yyyy-mm-dd} time{h:m:s,S} timestamp level [class] (thread) message
                parseDate(state); //skip state because log entry does not have it
                parseTime(state); //skip time, we have the timestamp
                return new LogEntry(lineNumber, parseTimeStamp(state), parseLevel(state), parseClass(state), parseThread(state),
                        parseMessage(state));
            default:
                throw new IllegalStateException();
        }
    }

    private boolean tryParse(LineState state) {
        try {
            switch (logFormat) {
                case FORMAT_1:
                case FORMAT_2:
                    parseDate(state);
                    break;
                case FORMAT_3:
                    parseTime(state);
                    break;
                default: throw new IllegalStateException();
            }
        } catch (ParseException e) {
            return false;
        } finally {
            state.reset();
        }
        return true;
    }

    private long parseTime(LineState state) throws ParseException {
        assertHasNext(state);
        StringBuilder builder = new StringBuilder();
        char c;
        while (state.hasNext() && (c = state.next()) != ' ') {
            builder.append(c);
        }
        return TIMESTAMP_PARSER.parse(builder.toString()).getTime();
    }

    private long parseDate(LineState state) throws ParseException {
        assertHasNext(state);
        StringBuilder builder = new StringBuilder();
        char c;
        while (state.hasNext() && (c = state.next()) != ' ') {
            builder.append(c);
        }
        return DATE_PARSER.parse(builder.toString()).getTime();
    }

    private long parseTimeStamp(LineState state) throws ParseException {
        assertHasNext(state);
        StringBuilder builder = new StringBuilder();
        char c;
        while (state.hasNext() && (c = state.next()) != ' ') {
            builder.append(c);
        }
        return Long.parseLong(builder.toString());
    }

    private String parseLevel(LineState state) {
        assertHasNext(state);
        StringBuilder builder = new StringBuilder();
        char c;
        while (state.hasNext() && (c = state.next()) != ' ') {
            builder.append(c);
        }
        return builder.toString().trim();
    }

    private String parseClass(LineState state) {
        assertHasNext(state);
        assertNextChar(state, '[');
        StringBuilder builder = new StringBuilder();
        char c;
        while (state.hasNext() && (c = state.next()) != ']') {
            builder.append(c);
        }
        state.skip(1); //skip space
        return builder.toString();
    }

    private String parseThread(LineState state) {
        assertHasNext(state);
        assertNextChar(state, '(');
        int level = 0;
        StringBuilder builder = new StringBuilder();
        char c;
        while (state.hasNext()) {
            c = state.next();
            if (level == 0 && c == ')') {
                break;
            } else if (c == '(') {
                level++;
            } else if (c == ')') {
                level--;
            }
            builder.append(c);
        }
        state.skip(1); //skip space
        return builder.toString();
    }

    private String parseMessage(LineState state) {
        StringBuilder builder = new StringBuilder();
        while (state.hasNext()) {
            builder.append(state.next());
        }
        return builder.toString();
    }

    private void assertNextChar(LineState state, char c) {
        boolean isSpaceExcepted = c == ' ';
        while (state.hasNext()) {
            char current = state.next();
            if (current == ' ' && isSpaceExcepted) {
                return;
            } else if (current == ' ') {
                continue; //skip spaces
            } else if (current == c) {
                return;
            }
            throw new IllegalStateException("Expected a " + c + " as next char but it is " + current +
                    ". State=" + state);
        }
    }

    private void assertHasNext(LineState state) {
        if (!state.hasNext()) {
            throw new IllegalStateException("Reached EOL soon as expected. State=" + state);
        }
    }

    private static enum LogFormat {
        FORMAT_1, FORMAT_2, FORMAT_3;

        static LogFormat fromString(String value) {
            if (value == null) {
                System.out.println("Format does not specified. Using format 1");
                return FORMAT_1;
            }
            try {
                int format = Integer.parseInt(value);
                switch (format) {
                    case 1:
                        return FORMAT_1;
                    case 2:
                        return FORMAT_2;
                    case 3:
                        return FORMAT_3;
                    default:
                        System.out.println("Unknown format " + value + " Using format 1");
                        return FORMAT_1;
                }
            } catch (Exception e) {
                System.out.println("Unknown format " + value + " Using format 1");
                return FORMAT_1;
            }
        }
    }

    private class LineState {
        private final char[] array;
        private int nextPosition;

        private LineState(char[] array) {
            this.array = array;
            this.nextPosition = 0;
        }

        public final char next() {
            if (!hasNext()) {
                throw new IllegalStateException("Reached EOL. State=" + this);
            }
            return array[nextPosition++];
        }

        public final boolean hasNext() {
            return nextPosition < array.length;
        }

        public final void skip(int chars) {
            nextPosition += chars;
        }

        public final void reset() {
            nextPosition = 0;
        }

        @Override
        public String toString() {
            return "LineState{" +
                    "line=" + new String(array) +
                    ", nextPosition=" + nextPosition +
                    '}';
        }
    }

    private class LogEntryIterator implements LogIterator {

        private final BufferedReader bufferedReader;
        private String currentLine;
        private int lineNumber;
        private LogEntry nextEntry = null;

        private LogEntryIterator(InputStream inputStream) throws Exception {
            this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            this.lineNumber = 0;
            nextLine();
            nextEntry();
        }

        @Override
        public final boolean hasNext() {
            return nextEntry != null;
        }

        @Override
        public final LogEntry next() throws Exception {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            LogEntry entry = nextEntry;
            nextEntry();
            return entry;
        }

        private void nextEntry() throws Exception {
            LogEntry entry = null;
            while (entry == null && currentLine != null) {
                entry = parseLine(lineNumber, currentLine);
                nextLine();
            }

            while (entry != null && currentLine != null && parseLine(entry, currentLine)) {
                nextLine();
            }
            nextEntry = entry;
        }

        private void nextLine() throws IOException {
            currentLine = bufferedReader.readLine();
            lineNumber++;
        }
    }
}
