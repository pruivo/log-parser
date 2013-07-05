package eu.cloudtm.parser;

import eu.cloudtm.LogEntry;

import java.io.InputStream;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public interface Parser {

    LogEntry[] parse(InputStream stream) throws Exception;

}
