package eu.cloudtm.analyzer;

import eu.cloudtm.LogEntry;

/**
 * @author Pedro Ruivo
 * @since 2.8
 */
public class AvgEventIntervalAnalyzer implements Analyzer {

    private long lastOpTimestamp;
    private int counter;

    @Override
    public void before() {
        lastOpTimestamp = -1;
        counter = 0;
        System.out.println("OpNumber,timestamp,duration");
    }

    @Override
    public void after() {
        //
    }

    @Override
    public void analyze(LogEntry logEntry) {
        if (logEntry.message().startsWith("Invoked with command GetKeyValueCommand")) {
            if (lastOpTimestamp == -1) {
                lastOpTimestamp = logEntry.time();
                System.out.println(counter++ + "," + logEntry.time() + ",0");
            } else {
                long duration = logEntry.time() - lastOpTimestamp;
                lastOpTimestamp = logEntry.time();
                System.out.println(counter++ + "," + logEntry.time() + "," + duration);
            }
        }
    }
}
