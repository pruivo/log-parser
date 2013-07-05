package eu.cloudtm;

import eu.cloudtm.analyzer.Analyzer;
import eu.cloudtm.parser.Parser;

import java.io.InputStream;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public class LogParser {

    public static void main(String[] args) throws Exception {
        Arguments arguments = new Arguments(Argument.values());
        arguments.parse(args);

        Parser parser = (Parser) Util.loadClass(arguments.get(Argument.PARSER)).newInstance();
        Analyzer analyzer = (Analyzer) Util.loadClass(arguments.get(Argument.ANALYZER)).newInstance();
        InputStream inputStream = Util.loadResource(arguments.get(Argument.FILE));

        if (inputStream == null) {
            throw new IllegalArgumentException(arguments.get(Argument.FILE) + " not found!");
        }

        LogEntry[] logEntries = parser.parse(inputStream);
        //System.out.println(Arrays.toString(logEntries));

        try {
            analyzer.before();
            for (LogEntry logEntry : logEntries) {
                analyzer.analyze(logEntry);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            analyzer.after();
        }

        System.exit(0);
    }

    private static enum Argument implements Arguments.Argument {
        PARSER(true, "Full class name of the parser. It must implement eu.cloudtm.parser.Parser interface", "--parser"),
        FILE(true, "Relative or full path of the log file", "--file"),
        ANALYZER(true, "Full class name of the analyze. It must implement eu.cloudtm.parser.Analyzer interface", "--analyzer");
        private final boolean hasValue;
        private final String help;
        private final String name;

        private Argument(boolean hasValue, String help, String name) {
            this.hasValue = hasValue;
            this.help = help;
            this.name = name;
        }

        @Override
        public boolean hasValue() {
            return hasValue;
        }

        @Override
        public boolean isValid(Arguments arguments) {
            switch (this) {
                case PARSER:
                    String className = arguments.get(name);
                    if (className == null) {
                        return false;
                    }
                    Class<?> clazz = Util.loadClass(arguments.get(name));
                    if (clazz == null || !Parser.class.isAssignableFrom(clazz)) {
                        return false;
                    }
                    break;
                case ANALYZER:
                    className = arguments.get(name);
                    if (className == null) {
                        return false;
                    }
                    clazz = Util.loadClass(arguments.get(name));
                    if (clazz == null || !Analyzer.class.isAssignableFrom(clazz)) {
                        return false;
                    }
                    break;
                case FILE:
                    return arguments.get(name) != null;
            }
            return true;
        }

        @Override
        public String help() {
            return help;
        }

        @Override
        public String consoleArgument() {
            return name;
        }
    }

}
