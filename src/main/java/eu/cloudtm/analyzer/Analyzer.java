package eu.cloudtm.analyzer;

import eu.cloudtm.LogEntry;

/**
 * //TODO: document this!
 *
 * @author Pedro Ruivo
 * @since 5.3
 */
public interface Analyzer {

    void before();

    void after();

    void analyze(LogEntry logEntry);
}
