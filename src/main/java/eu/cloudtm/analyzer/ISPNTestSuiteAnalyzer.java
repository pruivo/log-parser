package eu.cloudtm.analyzer;

import eu.cloudtm.LogEntry;

import java.io.*;
import java.util.*;

import static eu.cloudtm.Util.LINE_SEPARATOR;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public class ISPNTestSuiteAnalyzer implements Analyzer {

    private static final String CLASS_LIST_PROPERTY = "ispn.classes";
    private static final String METHOD_LIST_PROPERTY = "ispn.methods";
    private static final String WRITE_TO_FILE_PROPERTY = "writeToFile";
    private static final String FILE_NAME_FORMAT = "./%s.%s.log";
    private static final String FILE_NAME_FORMAT_IF_EXISTS = "./%s.%s(%s).log";
    private final Set<String> classList;
    private final Set<String> methodList;
    private final Map<String, TestState> testStateMap;
    private final boolean writeToFile;

    public ISPNTestSuiteAnalyzer() {
        writeToFile = Boolean.getBoolean(WRITE_TO_FILE_PROPERTY);
        String property = System.getProperty(CLASS_LIST_PROPERTY);
        if (property == null) {
            classList = Collections.emptySet();
        } else {
            classList = extractClasses(property);
        }
        property = System.getProperty(METHOD_LIST_PROPERTY);
        if (property == null) {
            methodList = Collections.emptySet();
        } else {
            methodList = new HashSet<String>(Arrays.asList(property.split(",")));
        }
        testStateMap = new HashMap<String, TestState>();
    }

    @Override
    public void before() {
        testStateMap.clear();
        System.out.println("filtering test classes: " + classList);
        System.out.println("filtering test methods: " + methodList);
        System.out.println("write to file? " + writeToFile);
    }

    @Override
    public void after() {
        //no-op
    }

    @Override
    public void analyze(LogEntry logEntry) {
        String testClass = classMatch(logEntry.thread());
        if (testClass == null) {
            return;
        }
        TestState state = getOrCreate(testClass);
        if (state.started) {
            state.printStream.println(logEntry.prettyPrint());
            state.finished = testFinished(logEntry.message());
            if (state.finished) {
                endTest(testClass, state);
            }
        } else {
            String testMethod = extractTestMethodIfStarting(logEntry.message());
            if (state.started = testStarted(testMethod)) {
                init(testClass, testMethod, state);
                state.printStream.println(logEntry.prettyPrint());
            }
        }
    }

    private Set<String> extractClasses(String classCommandSeparatedList) {
        String[] classArray = classCommandSeparatedList.split(",");
        Set<String> set = new HashSet<String>();
        for (String clazz : classArray) {
            int index = clazz.lastIndexOf('.');
            set.add(index == -1 ? clazz : clazz.substring(index + 1));
        }
        return set;
    }

    private void endTest(String testClass, TestState state) {
        if (state.printStream != null && writeToFile) {
            state.printStream.close();
        }
        testStateMap.remove(testClass);
    }

    private void init(String testClass, String testName, TestState state) {
        if (writeToFile) {
            File fileToWrite = createFile(testClass, testName);
            if (fileToWrite == null) {
                System.err.println("Cannot create file for " + testClass + "." + testName + ". Writing to STDOUT");
                state.printStream = System.out;
            } else {
                try {
                    state.printStream = new PrintStream(new FileOutputStream(fileToWrite));
                    System.out.println(testClass + "." + testName + " writing to " + fileToWrite.getAbsolutePath());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    state.printStream = System.out;
                }
            }
        } else {
            state.printStream = System.out;
        }
        //sanity check
        if (state.printStream == null) {
            throw new IllegalStateException("PrintStream cannot be null!!");
        }
    }

    private File createFile(String testClass, String testName) {
        File file = new File(String.format(FILE_NAME_FORMAT, testClass, testName));
        int i = 1;
        while (file.exists()) {
            file = new File(String.format(FILE_NAME_FORMAT_IF_EXISTS, testClass, testName, i++));
        }
        boolean success;
        try {
            success = file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }
        return success ? file : null;
    }

    private TestState getOrCreate(String testClass) {
        TestState testState = testStateMap.get(testClass);
        if (testState == null) {
            testState = new TestState();
            testStateMap.put(testClass, testState);
        }
        return testState;
    }

    private String classMatch(String threadName) {
        for (String clazz : classList) {
            if (threadName.contains(clazz)) {
                return clazz;
            }
        }
        return null;
    }

    private String extractTestMethodIfStarting(String message) {
        String line = message.split(LINE_SEPARATOR)[0];
        //Starting test testName(testClass)
        if (line.startsWith("Starting test")) {
            String method = line.split(" ")[2];
            return method.substring(0, method.indexOf('('));
        }
        return null;
    }

    private boolean testStarted(String testMethod) {
        return testMethod != null && methodList.contains(testMethod);
    }

    private boolean testFinished(String message) {
        boolean result = false;
        String line = message.split(LINE_SEPARATOR)[0];
        //Test testName(testClass) succeeded.|failed.
        if (line.startsWith("Test") && (line.endsWith("succeeded.") || line.endsWith("failed."))) {
            String method = line.split(" ")[1];
            String methodName = method.substring(0, method.indexOf('('));
            result = methodList.contains(methodName);
        }
        return result;
    }

    private class TestState {
        private boolean started;
        private boolean finished;
        private PrintStream printStream;
    }
}
