package eu.cloudtm.parser;

import eu.cloudtm.LogEntry;

import java.io.InputStream;
import java.util.Iterator;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public interface Parser {

    LogIterator parse(InputStream stream) throws Exception;

}
