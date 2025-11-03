package net.ravendb.client.documents.operations.AI;

import java.util.List;

/**
 * Settings for embedded AI models (placeholder for future implementation).
 */
public class EmbeddedSettings extends AbstractAiSettings {

    @Override
    public void validate(List<String> errors) {
    }

    @Override
    public AiSettingsCompareDifferences compare(AbstractAiSettings other) {
        if (other instanceof EmbeddedSettings) {
            return AiSettingsCompareDifferences.None;
        }

        return AiSettingsCompareDifferences.All;
    }
}
