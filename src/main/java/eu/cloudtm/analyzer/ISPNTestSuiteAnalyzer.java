package eu.cloudtm.analyzer;

import eu.cloudtm.LogEntry;
import eu.cloudtm.Util;

import java.util.*;

import static eu.cloudtm.Util.LINE_SEPARATOR;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public class ISPNTestSuiteAnalyzer implements Analyzer {

    private static final String CLASS_LIST_PROPERTY = "ispn.classes";
    private static final String METHOD_LIST_PROPERTY = "ispn.methods";
    private final Set<String> classList;
    private final Set<String> methodList;
    private final Map<String, TestState> testStateMap;

    public ISPNTestSuiteAnalyzer() {
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

    private Set<String> extractClasses(String classCommandSeparatedList) {
        String[] classArray = classCommandSeparatedList.split(",");
        Set<String> set = new HashSet<String>();
        for (String clazz : classArray) {
            int index = clazz.lastIndexOf('.');
            set.add(index == -1 ? clazz : clazz.substring(index + 1));
        }
        return set;
    }

    @Override
    public void before() {
        testStateMap.clear();
        System.out.println("filtering test classes: " + classList);
        System.out.println("filtering test methods: " + methodList);
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
        if (!testStateMap.containsKey(testClass)) {
            testStateMap.put(testClass, new TestState());
        }
        TestState state = testStateMap.get(testClass);
        if (state.started) {
            System.out.println(logEntry.prettyPrint());
            state.finished = testFinished(logEntry.message());
            if (state.finished) {
                testStateMap.remove(testClass);
            }
        } else {
            state.started = testStarted(logEntry.message());
            if (state.started) {
                System.out.println(logEntry.prettyPrint());
            }
        }
    }

    private String classMatch(String threadName) {
        for (String clazz : classList) {
            if (threadName.contains(clazz)) {
                return clazz;
            }
        }
        return null;
    }

    private boolean testStarted(String message) {
        boolean result = false;
        String line = message.split(LINE_SEPARATOR)[0];
        //Starting test testName(testClass)
        if (line.startsWith("Starting test")) {
            String method = line.split(" ")[2];
            String methodName = method.substring(0, method.indexOf('('));
            result = methodList.contains(methodName);
        }
        return result;
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
    }
}
