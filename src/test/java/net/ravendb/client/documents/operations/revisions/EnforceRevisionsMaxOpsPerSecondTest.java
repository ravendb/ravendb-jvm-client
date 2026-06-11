package net.ravendb.client.documents.operations.revisions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnforceRevisionsMaxOpsPerSecondTest {

    @Test
    public void acceptsPositiveValue() {
        EnforceRevisionsConfigurationOperation.Parameters parameters = new EnforceRevisionsConfigurationOperation.Parameters();
        parameters.setMaxOpsPerSecond(100);

        assertThat(parameters.getMaxOpsPerSecond()).isEqualTo(100);
    }

    @Test
    public void acceptsNullValue() {
        EnforceRevisionsConfigurationOperation.Parameters parameters = new EnforceRevisionsConfigurationOperation.Parameters();
        parameters.setMaxOpsPerSecond(null);

        assertThat(parameters.getMaxOpsPerSecond()).isNull();
    }

    @Test
    public void rejectsZeroOrNegativeValue() {
        EnforceRevisionsConfigurationOperation.Parameters parameters = new EnforceRevisionsConfigurationOperation.Parameters();

        assertThatThrownBy(() -> parameters.setMaxOpsPerSecond(0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MaxOpsPerSecond must be greater than 0");

        assertThatThrownBy(() -> parameters.setMaxOpsPerSecond(-5))
                .isInstanceOf(IllegalStateException.class);
    }
}
