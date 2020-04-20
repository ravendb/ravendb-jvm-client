package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.QueryData;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14811Test extends RemoteTestBase {

    @Test
    public void can_Project_Id_Field_In_Class() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user = new User();
            user.setName("Grisha");
            user.setAge(34);

            try (IDocumentSession session = store.openSession()) {
               session.store(user);
               session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                UserProjectionIntId result = session.query(User.class)
                        .selectFields(UserProjectionIntId.class, "name")
                        .firstOrDefault();

                assertThat(result)
                        .isNotNull();
                assertThat(result.getId())
                        .isZero();
                assertThat(result.getName())
                        .isEqualTo(user.getName());
            }

            try (IDocumentSession session = store.openSession()) {
                UserProjectionIntId result = session.query(User.class)
                        .selectFields(UserProjectionIntId.class,
                                new QueryData(new String[]{ "id" }, new String[] { "name" }))
                        .firstOrDefault();

                assertThat(result)
                        .isNotNull();
                assertThat(result.getId())
                        .isZero();
                assertThat(result.getName())
                        .isEqualTo(user.getId());
            }
        }
    }

    @Test
    public void can_project_id_field() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user = new User();
            user.setName("Grisha");
            user.setAge(34);

            try (IDocumentSession session = store.openSession()) {
                session.store(user);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                UserProjectionIntId result = session.query(User.class)
                        .selectFields(UserProjectionIntId.class, "name")
                        .firstOrDefault();

                assertThat(result)
                        .isNotNull();
                assertThat(result.getName())
                        .isEqualTo(user.getName());
            }

            try (IDocumentSession session = store.openSession()) {
                UserProjectionIntId result = session.query(User.class)
                        .selectFields(UserProjectionIntId.class,
                                new QueryData(new String[]{"age", "name"}, new String[]{"id", "name"}))
                        .firstOrDefault();

                assertThat(result)
                        .isNotNull();
                assertThat(result.getId())
                        .isEqualTo(user.getAge());
                assertThat(result.getName())
                        .isEqualTo(user.getName());

            }

            try (IDocumentSession session = store.openSession()) {
                UserProjectionStringId result = session.query(User.class)
                        .selectFields(UserProjectionStringId.class, "id", "name")
                        .firstOrDefault();

                assertThat(result)
                        .isNotNull();
                assertThat(result.getId())
                        .isEqualTo(user.getId());
                assertThat(result.getName())
                        .isEqualTo(user.getName());
            }

            try (IDocumentSession session = store.openSession()) {
                UserProjectionStringId result = session.query(User.class)
                        .selectFields(UserProjectionStringId.class,
                                new QueryData(new String[]{"name", "name"}, new String[]{"id", "name"}))
                        .firstOrDefault();

                assertThat(result)
                        .isNotNull();
                assertThat(result.getId())
                        .isEqualTo(user.getName());
                assertThat(result.getName())
                        .isEqualTo(user.getName());
            }
        }
    }

    public static class User {
        private String id;
        private String name;
        private int age;

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

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public static class UserProjectionIntId {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class UserProjectionStringId {
        private String id;
        private String name;

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
    }
}
