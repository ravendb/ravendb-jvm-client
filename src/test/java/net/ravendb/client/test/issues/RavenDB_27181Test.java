package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.AI.AiConnectionString;
import net.ravendb.client.documents.operations.AI.OpenAiReasoningEffort;
import net.ravendb.client.documents.operations.AI.OpenAiSettings;
import net.ravendb.client.documents.operations.connectionStrings.GetConnectionStringsOperation;
import net.ravendb.client.documents.operations.connectionStrings.GetConnectionStringsResult;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.serverwide.ConnectionStringType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_27181Test extends RemoteTestBase {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"High", "xhigh"})
    @EnableOnServer(thresholdVersion = "7.2.7")
    public void reasoningEffortSurvivesAConnectionStringRoundTrip(String reasoningEffort) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            OpenAiSettings settings = new OpenAiSettings("api-key", "https://api.openai.com/", "gpt-5-mini");
            settings.setReasoningEffort(reasoningEffort);

            AiConnectionString connectionString = new AiConnectionString();
            connectionString.setName("reasoning-effort");
            connectionString.setOpenAiSettings(settings);

            store.maintenance().send(new PutConnectionStringOperation<>(connectionString));

            GetConnectionStringsResult result = store.maintenance().send(
                    new GetConnectionStringsOperation("reasoning-effort", ConnectionStringType.AI));

            assertThat(result.getAiConnectionStrings().get("reasoning-effort").getOpenAiSettings().getReasoningEffort())
                    .isEqualTo(reasoningEffort);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void toReasoningEffortLowercasesTheLegacyEnum() {
        assertThat(OpenAiReasoningEffort.MINIMAL.toReasoningEffort()).isEqualTo("minimal");
        assertThat(OpenAiReasoningEffort.LOW.toReasoningEffort()).isEqualTo("low");
        assertThat(OpenAiReasoningEffort.MEDIUM.toReasoningEffort()).isEqualTo("medium");
        assertThat(OpenAiReasoningEffort.HIGH.toReasoningEffort()).isEqualTo("high");
    }
}
