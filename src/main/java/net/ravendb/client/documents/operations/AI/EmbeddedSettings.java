package net.ravendb.client.documents.operations.AI;

import java.util.EnumSet;
import java.util.List;

/**
 * Settings for embedded AI models (placeholder for future implementation).
 */
public final class EmbeddedSettings extends AbstractAiSettings {

    @Override
    public void validateFields(List<String> errors) {
    }

    @Override
    public EnumSet<AiSettingsCompareDifferences> compare(AbstractAiSettings other) {
        if (other instanceof EmbeddedSettings) {
            return EnumSet.of(AiSettingsCompareDifferences.None);
        }

        return EnumSet.of(AiSettingsCompareDifferences.All);
    }
}
