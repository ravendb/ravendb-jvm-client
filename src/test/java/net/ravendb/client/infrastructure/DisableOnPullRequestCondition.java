package net.ravendb.client.infrastructure;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisableOnPullRequestCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        String license = System.getenv("RAVEN_License");
        if (StringUtils.isNotEmpty(license)) {
            return ConditionEvaluationResult.enabled("Test enabled");
        }

        return ConditionEvaluationResult.disabled("Test disabled on Pull Request");
    }
}
