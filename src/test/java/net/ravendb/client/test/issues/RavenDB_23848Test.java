package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_23848Test extends RemoteTestBase {

    @Test
    public void should_generate_correct_rql_for_contains_all_with_empty_list() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                String rql = session.advanced().documentQuery(Course.class)
                        .whereEquals("ownerId", "test")
                        .containsAll("tags", new ArrayList<>())
                        .openSubclause()
                        .whereEquals("status", CourseStatus.PUBLISHED)
                        .orElse()
                        .whereEquals("status", CourseStatus.DRAFT)
                        .closeSubclause()
                        .toString();

                assertThat(rql).isEqualTo("from 'Courses' where ownerId = $p0 and true and (status = $p1 or status = $p2)");
            }
        }
    }

    public static class Course {
        private String ownerId;
        private String[] tags;
        private CourseStatus status;

        public String getOwnerId() {
            return ownerId;
        }

        public void setOwnerId(String ownerId) {
            this.ownerId = ownerId;
        }

        public String[] getTags() {
            return tags;
        }

        public void setTags(String[] tags) {
            this.tags = tags;
        }

        public CourseStatus getStatus() {
            return status;
        }

        public void setStatus(CourseStatus status) {
            this.status = status;
        }
    }

    public enum CourseStatus {
        DRAFT,
        PUBLISHED
    }
}
