package net.ravendb.client.infrastructure;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisableOn41ServerCondition implements ExecutionCondition {

    public static final String ENV_RAVENDB_SERVER_VERSION = "RAVENDB_SERVER_VERSION";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        String serverVersion = System.getenv(ENV_RAVENDB_SERVER_VERSION);
        if (StringUtils.isEmpty(serverVersion) || !"4.1".equalsIgnoreCase(serverVersion) ) {
            return ConditionEvaluationResult.enabled("Test enabled");
        }

        return ConditionEvaluationResult.disabled("Test disabled on RavenDB 4.1 Server");
    }
}
