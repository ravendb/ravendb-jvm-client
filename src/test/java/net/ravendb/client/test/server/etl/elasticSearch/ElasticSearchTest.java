package net.ravendb.client.test.server.etl.elasticSearch;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.GetOngoingTaskInfoOperation;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.etl.AddEtlOperation;
import net.ravendb.client.documents.operations.etl.AddEtlOperationResult;
import net.ravendb.client.documents.operations.etl.EtlType;
import net.ravendb.client.documents.operations.etl.Transformation;
import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchConnectionString;
import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchEtlConfiguration;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskElasticSearchEtlDetails;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class ElasticSearchTest extends RemoteTestBase {
    @Test
    public void canSetupElasticSearch() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            ElasticSearchConnectionString elasticSearchConnectionString = new ElasticSearchConnectionString();
            elasticSearchConnectionString.setName("e1");
            elasticSearchConnectionString.setNodes(new String[] { "http://127.0.0.1:8080" });

            store.maintenance().send(new PutConnectionStringOperation<>(elasticSearchConnectionString));

            ElasticSearchEtlConfiguration etlConfiguration = new ElasticSearchEtlConfiguration();
            etlConfiguration.setConnectionStringName("e1");
            Transformation transformation = new Transformation();
            transformation.setCollections(Collections.singletonList("Orders"));
            transformation.setScript("var userData = { UserId: id(this), Name: this.Name }; loadToTest(userData)");

            transformation.setName("Script #1");

            etlConfiguration.setTransforms(Collections.singletonList(transformation));


            AddEtlOperationResult etlResult = store.maintenance().send(new AddEtlOperation<>(etlConfiguration));

            OngoingTaskElasticSearchEtlDetails ongoingTask = (OngoingTaskElasticSearchEtlDetails) store.maintenance()
                    .send(new GetOngoingTaskInfoOperation(etlResult.getTaskId(), OngoingTaskType.ELASTIC_SEARCH_ETL));

            assertThat(ongoingTask)
                    .isNotNull();
            assertThat(ongoingTask.getConfiguration().getEtlType())
                    .isEqualTo(EtlType.ELASTIC_SEARCH);
            assertThat(ongoingTask.getConfiguration().getTransforms())
                    .hasSize(1);

        }
    }
}
