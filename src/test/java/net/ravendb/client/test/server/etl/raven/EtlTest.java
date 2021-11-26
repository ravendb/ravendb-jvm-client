package net.ravendb.client.test.server.etl.raven;

import net.ravendb.client.ReplicationTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.GetOngoingTaskInfoOperation;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringResult;
import net.ravendb.client.documents.operations.etl.*;
import net.ravendb.client.documents.operations.ongoingTasks.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.operations.ModifyOngoingTaskResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class EtlTest extends ReplicationTestBase {

    @Test
    public void canAddEtl() throws Exception {
        try (IDocumentStore src = getDocumentStore()) {
            try (IDocumentStore dst = getDocumentStore()) {
                insertDocument(src);
                PutConnectionStringResult result = createConnectionString(src, dst);

                assertThat(result)
                        .isNotNull();

                RavenEtlConfiguration etlConfiguration = new RavenEtlConfiguration();
                etlConfiguration.setConnectionStringName("toDst");
                etlConfiguration.setDisabled(false);
                etlConfiguration.setName("etlToDst");
                Transformation transformation = new Transformation();
                transformation.setApplyToAllDocuments(true);
                transformation.setName("Script #1");

                etlConfiguration.setTransforms(Collections.singletonList(transformation));

                AddEtlOperation<RavenConnectionString> operation = new AddEtlOperation<>(etlConfiguration);
                AddEtlOperationResult etlResult = src.maintenance().send(operation);

                assertThat(etlResult)
                        .isNotNull();

                assertThat(etlResult.getRaftCommandIndex())
                        .isPositive();

                assertThat(etlResult.getTaskId())
                        .isPositive();

                waitForDocumentToReplicate(dst, User.class, "users/1", 10* 1000);

                OngoingTaskRavenEtlDetails ongoingTask = (OngoingTaskRavenEtlDetails) src.maintenance()
                        .send(new GetOngoingTaskInfoOperation(etlResult.getTaskId(), OngoingTaskType.RAVEN_ETL));

                assertThat(ongoingTask)
                        .isNotNull();

                assertThat(ongoingTask.getTaskId())
                        .isEqualTo(etlResult.getTaskId());
                assertThat(ongoingTask.getTaskType())
                        .isEqualTo(OngoingTaskType.RAVEN_ETL);
                assertThat(ongoingTask.getResponsibleNode())
                        .isNotNull();
                assertThat(ongoingTask.getTaskState())
                        .isEqualTo(OngoingTaskState.ENABLED);
                assertThat(ongoingTask.getTaskName())
                        .isEqualTo("etlToDst");

                ModifyOngoingTaskResult deleteResult = src.maintenance()
                        .send(new DeleteOngoingTaskOperation(etlResult.getTaskId(), OngoingTaskType.RAVEN_ETL));

                assertThat(deleteResult.getTaskId())
                        .isEqualTo(etlResult.getTaskId());
            }
        }
    }

    @Test
    public void canAddEtlWithScript() throws Exception {
        try (IDocumentStore src = getDocumentStore()) {
            try (IDocumentStore dst = getDocumentStore()) {
                insertDocument(src);
                PutConnectionStringResult result = createConnectionString(src, dst);

                assertThat(result)
                        .isNotNull();

                RavenEtlConfiguration etlConfiguration = new RavenEtlConfiguration();
                etlConfiguration.setConnectionStringName("toDst");
                etlConfiguration.setDisabled(false);
                etlConfiguration.setName("etlToDst");
                Transformation transformation = new Transformation();
                transformation.setApplyToAllDocuments(false);
                transformation.setCollections(Collections.singletonList("Users"));
                transformation.setName("Script #1");
                transformation.setScript("loadToUsers(this);");

                etlConfiguration.setTransforms(Collections.singletonList(transformation));

                AddEtlOperation<RavenConnectionString> operation = new AddEtlOperation<>(etlConfiguration);
                AddEtlOperationResult etlResult = src.maintenance().send(operation);

                assertThat(etlResult)
                        .isNotNull();

                assertThat(etlResult.getRaftCommandIndex())
                        .isPositive();

                assertThat(etlResult.getTaskId())
                        .isPositive();

                waitForDocumentToReplicate(dst, User.class, "users/1", 10* 1000);
            }
        }
    }

    @Test
    public void canUpdateEtl() throws Exception {
        try (IDocumentStore src = getDocumentStore()) {
            try (IDocumentStore dst = getDocumentStore()) {
                insertDocument(src);
                PutConnectionStringResult result = createConnectionString(src, dst);

                assertThat(result)
                        .isNotNull();

                RavenEtlConfiguration etlConfiguration = new RavenEtlConfiguration();
                etlConfiguration.setConnectionStringName("toDst");
                etlConfiguration.setDisabled(false);
                etlConfiguration.setName("etlToDst");
                Transformation transformation = new Transformation();
                transformation.setApplyToAllDocuments(false);
                transformation.setCollections(Collections.singletonList("Users"));
                transformation.setName("Script #1");
                transformation.setScript("loadToUsers(this);");

                etlConfiguration.setTransforms(Collections.singletonList(transformation));

                AddEtlOperation<RavenConnectionString> operation = new AddEtlOperation<>(etlConfiguration);
                AddEtlOperationResult etlResult = src.maintenance().send(operation);

                waitForDocumentToReplicate(dst, User.class, "users/1", 10* 1000);

                // now change ETL configuration

                transformation.setCollections(Collections.singletonList("Cars"));
                transformation.setScript("loadToCars(this)");

                UpdateEtlOperationResult updateResult = src.maintenance().send(new UpdateEtlOperation<>(etlResult.getTaskId(), etlConfiguration));

                assertThat(updateResult)
                        .isNotNull();

                assertThat(updateResult.getRaftCommandIndex())
                        .isPositive();
                assertThat(updateResult.getTaskId())
                        .isGreaterThan(etlResult.getTaskId());

                // this document shouldn't be replicated via ETL
                try (IDocumentSession session = src.openSession()) {
                    User user1 = new User();
                    user1.setName("John");
                    session.store(user1, "users/2");
                    session.saveChanges();
                }

                waitForDocumentToReplicate(dst, User.class, "users/2", 4000);
            }
        }
    }

    @Test
    public void canResetEtlTask() throws Exception {
        try (IDocumentStore src = getDocumentStore()) {
            try (IDocumentStore dst = getDocumentStore()) {
                insertDocument(src);
                PutConnectionStringResult result = createConnectionString(src, dst);

                assertThat(result)
                        .isNotNull();

                RavenEtlConfiguration etlConfiguration = new RavenEtlConfiguration();
                etlConfiguration.setConnectionStringName("toDst");
                etlConfiguration.setDisabled(false);
                etlConfiguration.setName("etlToDst");
                Transformation transformation = new Transformation();
                transformation.setApplyToAllDocuments(true);
                transformation.setName("Script Q&A");

                etlConfiguration.setTransforms(Collections.singletonList(transformation));

                AddEtlOperation<RavenConnectionString> operation = new AddEtlOperation<>(etlConfiguration);
                AddEtlOperationResult etlResult = src.maintenance().send(operation);

                assertThat(etlResult)
                        .isNotNull();

                assertThat(etlResult.getRaftCommandIndex())
                        .isPositive();

                assertThat(etlResult.getTaskId())
                        .isPositive();

                waitForDocumentToReplicate(dst, User.class, "users/1", 10* 1000);

                try (IDocumentSession session = dst.openSession()) {
                    session.delete("users/1");
                }

                src.maintenance().send(new ResetEtlOperation("etlToDst", "Script Q&A"));

                // etl was reset - waiting again for users/1 doc
                waitForDocumentToReplicate(dst, User.class, "users/1", 10* 1000);
            }
        }
    }

    private PutConnectionStringResult createConnectionString(IDocumentStore src, IDocumentStore dst) {
        RavenConnectionString toDstLink = new RavenConnectionString();
        toDstLink.setDatabase(dst.getDatabase());
        toDstLink.setTopologyDiscoveryUrls(dst.getUrls());
        toDstLink.setName("toDst");

        return src.maintenance().send(new PutConnectionStringOperation<>(toDstLink));
    }

    private void insertDocument(IDocumentStore src) {
        try (IDocumentSession session = src.openSession()) {
            User user1 = new User();
            user1.setName("Marcin");
            session.store(user1, "users/1");
            session.saveChanges();
        }
    }
}
