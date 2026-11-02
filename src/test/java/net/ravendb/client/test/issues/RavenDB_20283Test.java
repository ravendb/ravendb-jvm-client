package net.ravendb.client.test.issues;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.batches.JsonPatchDocument;
import net.ravendb.client.documents.operations.JsonPatchOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_20283Test extends RemoteTestBase {

    @Test
    public void shouldPatchDocumentWithCamelCase() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                MyUser user = new MyUser();
                user.setUserName("john");
                user.setAge(10);
                session.store(user, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                MyUser user = session.query(MyUser.class).waitForNonStaleResults().single();

                // the JsonPatch value is serialized with the store's entity mapper (camelCase members)
                store.operations().send(new JsonPatchOperation("users/1", new JsonPatchDocument().add("/analytics", new Analytics(32))));

                session.advanced().patch(user, "age", 21);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ObjectNode user = session.load(ObjectNode.class, "users/1");
                assertThat(user.get("analytics").get("visits").asInt()).isEqualTo(32);
                assertThat(user.get("age").asInt()).isEqualTo(21);
                assertThat(user.get("userName").asText()).isEqualTo("john");
            }
        }
    }

    public static class MyUser {
        private String userName;
        private double age;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public double getAge() {
            return age;
        }

        public void setAge(double age) {
            this.age = age;
        }
    }

    public static class Analytics {
        private int visits;

        public Analytics() {
        }

        public Analytics(int visits) {
            this.visits = visits;
        }

        public int getVisits() {
            return visits;
        }

        public void setVisits(int visits) {
            this.visits = visits;
        }
    }
}
