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
public class TorqueBoxLogParser implements Parser {
    //format: time{h:m:s,S} level [class] (thread) message

    private static final DateFormat TIMESTAMP_PARSER = new SimpleDateFormat("HH:mm:ss,SSS");

    @Override
    public final LogIterator parse(InputStream inputStream) throws Exception {
        return new LogEntryIterator(inputStream);
    }

    private LogEntry parseLine(int lineNumber, String line) throws Exception {
        if (line == null || line.isEmpty()) {
            return null;
        }
        LineState state = new LineState(line.toCharArray());
        return new LogEntry(lineNumber, parseTimeStamp(state), parseLevel(state), parseClass(state), parseThread(state),
                parseMessage(state));
    }

    private long parseTimeStamp(LineState state) throws ParseException {
        assertHasNext(state);
        StringBuilder builder = new StringBuilder();
        char c;
        while (state.hasNext() && (c = state.next()) != ' ') {
            builder.append(c);
        }
        return TIMESTAMP_PARSER.parse(builder.toString()).getTime();
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

        private LogEntryIterator(InputStream inputStream) throws IOException {
            this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            this.lineNumber = 0;
            nextLine();
        }

        @Override
        public final boolean hasNext() {
            return currentLine != null && !currentLine.isEmpty();
        }

        @Override
        public final LogEntry next() throws Exception {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            LogEntry entry = parseLine(lineNumber, currentLine);
            nextLine();
            return entry;
        }

        private void nextLine() throws IOException {
            this.currentLine = bufferedReader.readLine();
            this.lineNumber++;
        }
    }
}
