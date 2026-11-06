package net.ravendb.client.documents.operations.revisions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.GetOperationStateOperation;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@EnableOnServer(thresholdVersion = "7.2")
public class RevisionsOperationsResultsTest extends RemoteTestBase {

    private static void setupRevisionsAndData(IDocumentStore store) {
        RevisionsCollectionConfiguration defaultConfig = new RevisionsCollectionConfiguration();
        defaultConfig.setDisabled(false);
        RevisionsConfiguration configuration = new RevisionsConfiguration();
        configuration.setDefaultConfig(defaultConfig);
        store.maintenance().send(new ConfigureRevisionsOperation(configuration));

        try (IDocumentSession session = store.openSession()) {
            User user = new User();
            user.setName("John");
            session.store(user, "users/1");
            session.saveChanges();

            user.setName("Bob");
            session.saveChanges();
        }
    }

    private static <T> T resultOf(IDocumentStore store, Operation operation, Class<T> clazz) {
        operation.waitForCompletion();
        ObjectNode state = store.maintenance().send(new GetOperationStateOperation(operation.getId()));
        return JsonExtensions.getDefaultMapper().convertValue(state.get("Result"), clazz);
    }

    @Test
    public void canAdoptOrphanedRevisions() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setupRevisionsAndData(store);

            AdoptOrphanedRevisionsOperation.Parameters parameters = new AdoptOrphanedRevisionsOperation.Parameters();
            parameters.setCollections(new String[]{"Users"});

            Operation operation = store.operations().sendAsync(new AdoptOrphanedRevisionsOperation(parameters));
            AdoptOrphanedRevisionsResult result = resultOf(store, operation, AdoptOrphanedRevisionsResult.class);

            assertThat(result.getAdoptedCount()).isZero();
            assertThat(result.getScannedDocuments()).isEqualTo(1);
            assertThat(result.getLastProcessedEtags()).containsKey(store.getDatabase());
            assertThat(result.getEtagBarriersUsed()).containsKey(store.getDatabase());
        }
    }

    @Test
    public void canEnforceConfigurationAndResumeFromContinuation() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setupRevisionsAndData(store);

            Operation operation = store.operations().sendAsync(new EnforceRevisionsConfigurationOperation());
            EnforceConfigurationResult result = resultOf(store, operation, EnforceConfigurationResult.class);

            assertThat(result.getScannedDocuments()).isEqualTo(1);
            assertThat(result.getScannedRevisions()).isEqualTo(2);
            assertThat(result.getRemovedRevisions()).isZero();
            assertThat(result.getNodeTags()).containsKey(store.getDatabase());

            RevisionsOperationContinuationParameters continuation = new RevisionsOperationContinuationParameters();
            continuation.setStartFromEtags(result.getLastProcessedEtags());
            continuation.setEtagBarriers(result.getEtagBarriersUsed());
            continuation.setNodeTags(result.getNodeTags());

            EnforceRevisionsConfigurationOperation.Parameters parameters = new EnforceRevisionsConfigurationOperation.Parameters();
            parameters.setContinuationParameters(continuation);

            Operation resumed = store.operations().sendAsync(new EnforceRevisionsConfigurationOperation(parameters));
            EnforceConfigurationResult resumedResult = resultOf(store, resumed, EnforceConfigurationResult.class);

            assertThat(resumedResult.getScannedDocuments()).isZero();
        }
    }
}
