package net.ravendb.client.infrastructure;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisableOnJava8Condition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        int version = getVersion();

        return version >= 9 ? ConditionEvaluationResult.enabled(null) : ConditionEvaluationResult.disabled("Test disabled in Java <= 8");
    }

    private static int getVersion() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) {
                version = version.substring(0, dot);
            }
        }

        return Integer.parseInt(version);
    }
}
