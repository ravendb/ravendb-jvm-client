package net.ravendb.client.infrastructure;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DisableOnPullRequestCondition implements ExecutionCondition {

    public static final String ENV_TRAVIS_PULL_REQUEST = "TRAVIS_PULL_REQUEST";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        String travisPull = System.getenv(ENV_TRAVIS_PULL_REQUEST);
        if (StringUtils.isEmpty(travisPull) || "false".equalsIgnoreCase(travisPull) ) {
            return ConditionEvaluationResult.enabled("Test enabled");
        }

        return ConditionEvaluationResult.disabled("Test disabled on Travis Pull Request");
    }
}
