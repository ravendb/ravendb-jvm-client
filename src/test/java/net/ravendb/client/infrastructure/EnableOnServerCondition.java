package net.ravendb.client.infrastructure;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import java.util.Optional;

public class EnableOnServerCondition implements ExecutionCondition {

    public static final String ENV_RAVENDB_SERVER_VERSION = "RAVENDB_SERVER_VERSION";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<EnableOnServer> annotation = AnnotationSupport.findAnnotation(context.getElement(), EnableOnServer.class);

        if (annotation.isPresent()) {
            String threshold = annotation.get().thresholdVersion();
            String serverVersion = System.getenv(ENV_RAVENDB_SERVER_VERSION);

            if (serverVersion.compareTo(threshold) < 0) {
                return ConditionEvaluationResult.disabled("Disabled: server version " + serverVersion + " < " + threshold);
            }
        }

        return ConditionEvaluationResult.enabled("Test enabled");
    }
}
