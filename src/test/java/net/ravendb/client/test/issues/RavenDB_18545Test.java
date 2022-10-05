package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_18545Test extends RemoteTestBase {

    @Test
    public void quotationForGroupInAlias() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Job job = new Job();
                job.setName("HR Worker");
                job.setGroup("HR");

                session.store(job);

                String jobId = job.getId();

                session.saveChanges();

                IDocumentQuery<Job> q = session.query(Job.class)
                        .groupBy("group")
                        .selectKey(null, "group")
                        .selectCount();

                assertThat(q.toString())
                        .contains("as 'group'");

                List<Job> l = q.toList();

                assertThat(l)
                        .isNotEmpty();

                assertThat(l.get(0).getGroup())
                        .isEqualTo(job.getGroup());
            }
        }
    }

    public static class Job {
        private String id;
        private String name;
        private String group;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }
    }
}
