package net.ravendb.client.test.server.etl.sql;

import net.ravendb.client.ReplicationTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.GetOngoingTaskInfoOperation;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringResult;
import net.ravendb.client.documents.operations.etl.*;
import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;
import net.ravendb.client.documents.operations.etl.sql.SqlEtlConfiguration;
import net.ravendb.client.documents.operations.etl.sql.SqlEtlTable;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskSqlEtlDetails;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskState;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class SqlTest extends ReplicationTestBase {

    @Test
    public void canAddEtl() throws Exception {
        try (IDocumentStore src = getDocumentStore()) {
            insertDocument(src);
            PutConnectionStringResult result = createConnectionString(src);

            assertThat(result)
                    .isNotNull();

            SqlEtlConfiguration etlConfiguration = new SqlEtlConfiguration();
            etlConfiguration.setConnectionStringName("toDst");
            etlConfiguration.setDisabled(false);
            etlConfiguration.setName("etlToDst");
            Transformation transformation = new Transformation();
            transformation.setApplyToAllDocuments(true);
            transformation.setName("Script #1");
            transformation.setScript("loadToUsers(this)");

            SqlEtlTable table1 = new SqlEtlTable();
            table1.setDocumentIdColumn("Id");
            table1.setInsertOnlyMode(false);
            table1.setTableName("Users");

            etlConfiguration.setSqlTables(Collections.singletonList(table1));
            etlConfiguration.setTransforms(Collections.singletonList(transformation));

            AddEtlOperation<SqlConnectionString> operation = new AddEtlOperation<>(etlConfiguration);
            AddEtlOperationResult etlResult = src.maintenance().send(operation);

            assertThat(etlResult)
                    .isNotNull();

            assertThat(etlResult.getRaftCommandIndex())
                    .isPositive();

            assertThat(etlResult.getTaskId())
                    .isPositive();

            // and try to read ongoing sql task

            OngoingTaskSqlEtlDetails ongoingTask = (OngoingTaskSqlEtlDetails) src.maintenance()
                    .send(new GetOngoingTaskInfoOperation(etlResult.getTaskId(), OngoingTaskType.SQL_ETL));

            assertThat(ongoingTask)
                    .isNotNull();

            assertThat(ongoingTask.getTaskId())
                    .isEqualTo(etlResult.getTaskId());
            assertThat(ongoingTask.getTaskType())
                    .isEqualTo(OngoingTaskType.SQL_ETL);
            assertThat(ongoingTask.getResponsibleNode())
                    .isNotNull();
            assertThat(ongoingTask.getTaskState())
                    .isEqualTo(OngoingTaskState.ENABLED);
            assertThat(ongoingTask.getTaskName())
                    .isEqualTo("etlToDst");

            SqlEtlConfiguration configuration = ongoingTask.getConfiguration();
            List<Transformation> transforms = configuration.getTransforms();
            assertThat(transforms)
                    .hasSize(1);
            assertThat(transforms.get(0).isApplyToAllDocuments())
                    .isTrue();

            assertThat(configuration.getSqlTables())
                    .hasSize(1);
            assertThat(configuration.getSqlTables().get(0).getTableName())
                    .isEqualTo("Users");
        }
    }

    @Test
    public void canAddEtlWithScript() throws Exception {
        try (IDocumentStore src = getDocumentStore()) {
            insertDocument(src);
            PutConnectionStringResult result = createConnectionString(src);

            assertThat(result)
                    .isNotNull();

            SqlEtlConfiguration etlConfiguration = new SqlEtlConfiguration();
            etlConfiguration.setConnectionStringName("toDst");
            etlConfiguration.setDisabled(false);
            etlConfiguration.setName("etlToDst");
            Transformation transformation = new Transformation();
            transformation.setApplyToAllDocuments(false);
            transformation.setCollections(Collections.singletonList("Users"));
            transformation.setName("Script #1");
            transformation.setScript("loadToUsers(this);");

            SqlEtlTable table1 = new SqlEtlTable();
            table1.setDocumentIdColumn("Id");
            table1.setInsertOnlyMode(false);
            table1.setTableName("Users");

            etlConfiguration.setSqlTables(Collections.singletonList(table1));
            etlConfiguration.setTransforms(Collections.singletonList(transformation));

            AddEtlOperation<SqlConnectionString> operation = new AddEtlOperation<>(etlConfiguration);
            AddEtlOperationResult etlResult = src.maintenance().send(operation);

            assertThat(etlResult)
                    .isNotNull();

            assertThat(etlResult.getRaftCommandIndex())
                    .isPositive();

            assertThat(etlResult.getTaskId())
                    .isPositive();
        }
    }

    @Test
    public void canUpdateEtl() throws Exception {
        try (IDocumentStore src = getDocumentStore()) {
            insertDocument(src);
            PutConnectionStringResult result = createConnectionString(src);

            assertThat(result)
                    .isNotNull();

            SqlEtlConfiguration etlConfiguration = new SqlEtlConfiguration();
            etlConfiguration.setConnectionStringName("toDst");
            etlConfiguration.setDisabled(false);
            etlConfiguration.setName("etlToDst");
            Transformation transformation = new Transformation();
            transformation.setApplyToAllDocuments(false);
            transformation.setCollections(Collections.singletonList("Users"));
            transformation.setName("Script #1");
            transformation.setScript("loadToUsers(this);");

            SqlEtlTable table1 = new SqlEtlTable();
            table1.setDocumentIdColumn("Id");
            table1.setInsertOnlyMode(false);
            table1.setTableName("Users");

            etlConfiguration.setSqlTables(Collections.singletonList(table1));
            etlConfiguration.setTransforms(Collections.singletonList(transformation));

            AddEtlOperation<SqlConnectionString> operation = new AddEtlOperation<>(etlConfiguration);
            AddEtlOperationResult etlResult = src.maintenance().send(operation);

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
        }
    }

    @Test
    public void canResetEtlTask() throws Exception {
        try (IDocumentStore src = getDocumentStore()) {
            insertDocument(src);
            PutConnectionStringResult result = createConnectionString(src);

            assertThat(result)
                    .isNotNull();

            SqlEtlConfiguration etlConfiguration = new SqlEtlConfiguration();
            etlConfiguration.setConnectionStringName("toDst");
            etlConfiguration.setDisabled(false);
            etlConfiguration.setName("etlToDst");
            Transformation transformation = new Transformation();
            transformation.setApplyToAllDocuments(true);
            transformation.setName("Script Q&A");
            transformation.setScript("loadToUsers(this)");

            SqlEtlTable table1 = new SqlEtlTable();
            table1.setDocumentIdColumn("Id");
            table1.setInsertOnlyMode(false);
            table1.setTableName("Users");

            etlConfiguration.setSqlTables(Collections.singletonList(table1));
            etlConfiguration.setTransforms(Collections.singletonList(transformation));

            AddEtlOperation<SqlConnectionString> operation = new AddEtlOperation<>(etlConfiguration);
            AddEtlOperationResult etlResult = src.maintenance().send(operation);

            assertThat(etlResult)
                    .isNotNull();

            assertThat(etlResult.getRaftCommandIndex())
                    .isPositive();

            assertThat(etlResult.getTaskId())
                    .isPositive();

            src.maintenance().send(new ResetEtlOperation("etlToDst", "Script Q&A"));

            // we don't assert against real database
        }
    }

    private PutConnectionStringResult createConnectionString(IDocumentStore src) {
        SqlConnectionString toDstLink = new SqlConnectionString();
        toDstLink.setName("toDst");
        toDstLink.setFactoryName("MySql.Data.MySqlClient");
        toDstLink.setConnectionString("hostname=localhost;user=root;password=");

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
