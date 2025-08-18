package net.ravendb.client.infrastructure;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class EnableOn70ServerCondition implements ExecutionCondition {

    public static final String ENV_RAVENDB_SERVER_VERSION = "RAVENDB_SERVER_VERSION";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        String ravenServerVersion = System.getenv(ENV_RAVENDB_SERVER_VERSION);

        if (StringUtils.isEmpty(ravenServerVersion) || ravenServerVersion.compareTo("7.0") < 0) {
            return ConditionEvaluationResult.disabled("Test disabled on server lower than 7.0");
        }

        return ConditionEvaluationResult.enabled("Test enabled");
    }
}
