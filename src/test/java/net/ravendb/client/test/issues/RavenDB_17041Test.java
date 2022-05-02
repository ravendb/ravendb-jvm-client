package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_17041Test extends RemoteTestBase {

    @Test
    public void can_Include_Secondary_Level_With_Alias() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            UserIndex userIndex = new UserIndex();
            userIndex.execute(store);

            try (IDocumentSession session = store.openSession()) {
                RoleData role1 = new RoleData();
                role1.setName("admin");
                role1.setRole("role/1");

                RoleData role2 = new RoleData();
                role2.setName("developer");
                role2.setRole("role/2");

                List<RoleData> roles = Arrays.asList(role1, role2);
                User user = new User();
                user.setFirstName("Rhinos");
                user.setLastName("Hiber");
                user.setRoles(roles);

                session.store(role1, role1.role);
                session.store(role2, role2.role);
                session.store(user);
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                String query = "from index 'UserIndex' as u " +
                        "select { firstName : u.firstName, " +
                        "lastName : u.lastName, " +
                        "roles : u.roles.map(function(r){return {role:r.role};}) } " +
                        "include 'u.roles[].role'";
                List<User> users = session.advanced().rawQuery(User.class, query)
                        .toList();

                assertThat(users)
                        .hasSize(1);

                RoleData loaded = session.load(RoleData.class, "role/1");
                assertThat(session.advanced().getNumberOfRequests())
                        .isOne();
                assertThat(loaded.getRole())
                        .isEqualTo("role/1");

                loaded = session.load(RoleData.class, "role/2");
                assertThat(session.advanced().getNumberOfRequests())
                        .isOne();
                assertThat(loaded.getRole())
                        .isEqualTo("role/2");
            }
        }
    }

    public static class UserIndex extends AbstractIndexCreationTask {
        public UserIndex() {
            map = "from u in docs.Users select new { u.firstName, u.lastName, u.roles }";
        }
    }

    public static class RoleData {
        private String name;
        private String role;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class User {
        private String firstName;
        private String lastName;
        private List<RoleData> roles;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public List<RoleData> getRoles() {
            return roles;
        }

        public void setRoles(List<RoleData> roles) {
            this.roles = roles;
        }
    }

}
