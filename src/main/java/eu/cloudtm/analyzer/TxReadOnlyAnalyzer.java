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
public class TxReadOnlyAnalyzer implements Analyzer {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    public TxReadOnlyAnalyzer() {
    }

    private static String prettyPrintTime(long time) {
        if (time == -1) {
            return "N/A";
        }
        return DATE_FORMAT.format(new Date(time));
    }

    @Override
    public void before() {
        System.out.println("timestamp(ms),timestamp(date),duration(nanoseconds)");
    }

    @Override
    public void after() {
    }

    @Override
    public void analyze(LogEntry logEntry) {
        String message = logEntry.message();
        if (message.startsWith("Add") && message.endsWith("RO_TX_SUCCESSFUL_EXECUTION_TIME")) {
            String[] split = message.split(" ");
            double duration = Double.parseDouble(split[1]);
            System.out.println(logEntry.time() + "," + prettyPrintTime(logEntry.time()) + "," + duration);
        }
        //RO_TX_SUCCESSFUL_EXECUTION_TIME
    }
}
