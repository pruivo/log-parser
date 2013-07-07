package eu.cloudtm.parser;

import eu.cloudtm.LogEntry;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public interface LogIterator {

    boolean hasNext() throws Exception;

    LogEntry next() throws Exception;

}
