package net.ravendb.client.test.mailingList;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.queries.facets.FacetResult;
import net.ravendb.client.documents.queries.facets.IAggregationDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LazyAggregationEmbedded extends RemoteTestBase {

    @Test
    public void test() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                Task task1 = new Task();
                task1.setAssigneeId("users/1");
                task1.setId("tasks/1");

                Task task2 = new Task();
                task2.setAssigneeId("users/1");
                task2.setId("tasks/2");

                Task task3 = new Task();
                task3.setAssigneeId("users/2");
                task3.setId("tasks/3");

                session.store(task1);
                session.store(task2);
                session.store(task3);
                session.saveChanges();

                new TaskIndex().execute(store);

                waitForIndexing(store);

                IAggregationDocumentQuery<Task> query = session.query(Task.class, TaskIndex.class)
                        .aggregateBy(f -> f.byField("assigneeId").withDisplayName("assigneeId"));

                Lazy<Map<String, FacetResult>> lazyOperation = query.executeLazy();
                Map<String, FacetResult> facetValue = lazyOperation.getValue();

                Map<String, Integer> userStats = new HashMap<>();
                facetValue.get("assigneeId").getValues().forEach(value -> userStats.put(value.getRange(), value.getCount()));

                assertThat(userStats.get("users/1"))
                        .isEqualTo(2);

                assertThat(userStats.get("users/2"))
                        .isEqualTo(1);
            }
        }
    }

    public static class TaskIndex extends AbstractIndexCreationTask {
        public TaskIndex() {
            map = " from task in docs.Tasks select new { task.assigneeId } ";
        }
    }

    public static class Task {
        private String id;
        private String assigneeId;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAssigneeId() {
            return assigneeId;
        }

        public void setAssigneeId(String assigneeId) {
            this.assigneeId = assigneeId;
        }
    }
}
