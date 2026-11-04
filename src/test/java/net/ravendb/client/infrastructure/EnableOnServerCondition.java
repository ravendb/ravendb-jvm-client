package net.ravendb.client.infrastructure;

import org.apache.commons.lang3.StringUtils;
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

            if (StringUtils.isNotBlank(serverVersion) && !isAtLeast(serverVersion.trim(), threshold)) {
                return ConditionEvaluationResult.disabled("Disabled: server version " + serverVersion + " < " + threshold);
            }
        }

        return ConditionEvaluationResult.enabled("Test enabled");
    }

    /**
     * Compares versions numerically, component by component (so 7.2.10 is newer than 7.2.2).
     * A server version that names only a release line (e.g. "7.2", as CI sets it for the latest
     * nightly of that line) satisfies every threshold within that line.
     */
    static boolean isAtLeast(String serverVersion, String threshold) {
        String[] server = serverVersion.split("\\.");
        String[] required = threshold.split("\\.");

        for (int i = 0; i < Math.min(server.length, required.length); i++) {
            int diff = Integer.compare(leadingNumber(server[i]), leadingNumber(required[i]));
            if (diff != 0) {
                return diff > 0;
            }
        }

        return true;
    }

    private static int leadingNumber(String component) {
        int end = 0;
        while (end < component.length() && Character.isDigit(component.charAt(end))) {
            end++;
        }

        return end == 0 ? 0 : Integer.parseInt(component.substring(0, end));
    }
}
