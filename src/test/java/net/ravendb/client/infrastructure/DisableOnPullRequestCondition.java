package net.ravendb.client.infrastructure;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisableOnPullRequestCondition implements ExecutionCondition {

    public static final String ENV_RAVEN_LICENSE = "RAVEN_LICENSE";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        String ravenLicense = System.getenv(ENV_RAVEN_LICENSE);
        if (StringUtils.isNotEmpty(ravenLicense)) {
            return ConditionEvaluationResult.enabled("Test enabled");
        }

        return ConditionEvaluationResult.disabled("Test disabled on Pull Request");
    }
}
