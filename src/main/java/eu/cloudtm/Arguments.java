package eu.cloudtm;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public class Arguments {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private final Argument[] keys;
    private final String[] values;
    private final int maxArgumentNameLength;

    public Arguments(Collection<Argument> argumentCollection) {
        keys = new Argument[argumentCollection.size()];
        argumentCollection.toArray(keys);
        values = new String[keys.length];
        maxArgumentNameLength = assertNoDuplicatedAndCalculateMaximumLength();
    }

    public Arguments(Argument[] arguments) {
        keys = new Argument[arguments.length];
        values = new String[arguments.length];
        System.arraycopy(arguments, 0, keys, 0, keys.length);
        maxArgumentNameLength = assertNoDuplicatedAndCalculateMaximumLength();
    }

    public synchronized final void parse(final String[] consoleArguments) {
        if (consoleArguments == null || consoleArguments.length == 0) {
            validate();
            return;
        }
        for (int i = 0; i < consoleArguments.length; ++i) {
            int index = find(consoleArguments[i]);
            if (index == -1) {
                invalidArgument(consoleArguments[i], true);
            }
            Argument argument = keys[index];
            if (argument.hasValue()) {
                assertValue(consoleArguments, i);
                values[index] = consoleArguments[++i];
            } else {
                values[index] = Boolean.TRUE.toString();
            }
        }
        validate();
    }

    public final String help() {
        StringBuilder builder = new StringBuilder();
        for (Argument argument : keys) {
            builder.append("  ").append(argumentName(argument)).append(" ");
            if (argument.hasValue()) {
                builder.append("<value> ");
            } else {
                builder.append("        ");
            }
            builder.append(argument.help());
            builder.append(LINE_SEPARATOR);
        }
        return builder.toString();
    }

    public final String get(Argument argument) {
        assertNotNull(argument, "Argument");
        return get(argument.consoleArgument());
    }

    public final String get(String consoleArgument) {
        assertNotNull(consoleArgument, "Argument");
        int index = find(consoleArgument);
        if (index == -1) {
            invalidArgument(consoleArgument, false);
        }
        return values[index];
    }

    public final Boolean getAsBoolean(Argument argument) {
        assertNotNull(argument, "Argument");
        return getAsBoolean(argument.consoleArgument());
    }

    public final Boolean getAsBoolean(String consoleArgument) {
        assertNotNull(consoleArgument, "Argument");
        int index = find(consoleArgument);
        if (index == -1) {
            invalidArgument(consoleArgument, false);
        }
        return Boolean.valueOf(values[index]);
    }

    public final Number getAsNumber(Argument argument) throws ParseException {
        assertNotNull(argument, "Argument");
        return getAsNumber(argument.consoleArgument());
    }

    public final Number getAsNumber(String consoleArgument) throws ParseException {
        assertNotNull(consoleArgument, "Argument");
        int index = find(consoleArgument);
        if (index == -1) {
            invalidArgument(consoleArgument, false);
        }
        return NumberFormat.getInstance().parse(values[index]);
    }

    private void validate() {
        for (Argument argument : keys) {
            if (!argument.isValid(this)) {
                throw new IllegalArgumentException(argument.consoleArgument() + " has an invalid value");
            }
        }
    }

    private String argumentName(Argument argument) {
        StringBuilder builder = new StringBuilder(maxArgumentNameLength);
        builder.append(argument.consoleArgument());
        while (builder.length() < maxArgumentNameLength) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private void assertNotNull(Object argument, String object) {
        if (argument == null) {
            throw new NullPointerException(object + " cannot be null!");
        }
    }

    private void assertValue(String[] consoleArguments, int i) {
        if (i + 1 >= consoleArguments.length) {
            throw new IllegalArgumentException(consoleArguments[i] + " expected a value!");
        }
    }

    private void invalidArgument(String consoleArgument, boolean help) {
        if (help) {
            System.err.println(help);
        }
        throw new IllegalArgumentException(consoleArgument + " not found!");
    }

    private int find(final String consoleArgument) {
        for (int i = 0; i < keys.length; ++i) {
            if (keys[i].consoleArgument().equals(consoleArgument)) {
                return i;
            }
        }
        return -1;
    }

    private int assertNoDuplicatedAndCalculateMaximumLength() {
        Set<String> set = new HashSet<String>();
        int maxLenght = 0;
        for (Argument argument : keys) {
            String consoleArgument = argument.consoleArgument();
            if (!set.add(consoleArgument)) {
                throw new IllegalArgumentException("Duplicated console argument: " + consoleArgument);
            }
            if (maxLenght < consoleArgument.length()) {
                maxLenght = consoleArgument.length();
            }
        }
        return maxLenght;
    }

    public static interface Argument {

        boolean hasValue();

        boolean isValid(Arguments arguments);

        String help();

        String consoleArgument();

    }

}
