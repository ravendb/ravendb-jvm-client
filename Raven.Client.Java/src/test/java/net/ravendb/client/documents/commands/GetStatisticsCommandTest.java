package net.ravendb.client.documents.commands;

import com.google.common.base.Stopwatch;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetNextOperationIdCommand;
import net.ravendb.client.documents.commands.GetStatisticsCommand;
import net.ravendb.client.documents.operations.DatabaseStatistics;
import net.ravendb.client.documents.operations.IndexInformation;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.CreateSampleDataOperation;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class GetStatisticsCommandTest extends RemoteTestBase {

    @Test
    public void canGetStats() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {

            RequestExecutor executor = store.getRequestExecutor();

            //TODO: this can take a lot of time - consumer creating less data for this test
            CreateSampleDataOperation sampleData = new CreateSampleDataOperation();
            store.admin().send(sampleData);

            waitForIndexing(store, store.getDatabase(), null);

            GetStatisticsCommand command = new GetStatisticsCommand();
            executor.execute(command);

            DatabaseStatistics stats = command.getResult();
            assertThat(stats)
                    .isNotNull();

            assertThat(stats.getLastDocEtag())
                    .isNotNull()
                    .isGreaterThan(0);

            assertThat(stats.getCountOfIndexes())
                    .isEqualTo(3);

            assertThat(stats.getCountOfDocuments())
                    .isEqualTo(1059);

            assertThat(stats.getCountOfRevisionDocuments())
                    .isEqualTo(0);

            assertThat(stats.getCountOfDocumentsConflicts())
                    .isEqualTo(0);

            assertThat(stats.getCountOfConflicts())
                    .isEqualTo(0);

            assertThat(stats.getCountOfUniqueAttachments())
                    .isEqualTo(0);

            assertThat(stats.getDatabaseChangeVector())
                    .isNotEmpty();

            assertThat(stats.getDatabaseId())
                    .isNotEmpty();

            assertThat(stats.getPager())
                    .isNotEmpty();

            assertThat(stats.getLastIndexingTime())
                    .isNotNull();

            assertThat(stats.getIndexes())
                    .isNotNull();

            for (IndexInformation indexInformation : stats.getIndexes()) {
                assertThat(indexInformation.getEtag())
                        .isNotNull();

                assertThat(indexInformation.getName())
                        .isNotNull();

                assertThat(indexInformation.getIsStale())
                        .isFalse();

                assertThat(indexInformation.getState())
                        .isNotNull();

                assertThat(indexInformation.getLockMode())
                        .isNotNull();

                assertThat(indexInformation.getPriority())
                        .isNotNull();

                assertThat(indexInformation.getType())
                        .isNotNull();

                assertThat(indexInformation.getLastIndexingTime())
                        .isNotNull();
            }
        }
    }
}
