package net.ravendb.client.test.session;

import net.ravendb.client.RavenTestHelper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class AddOrPatchTest extends RemoteTestBase {

    @Test
    public void canAddOrPatch() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = "users/1";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setFirstName("Hibernating");
                user.setLastName("Rhinos");
                user.setLastLogin(new Date());
                session.store(user, id);
                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }

            try (IDocumentSession session = store.openSession()) {
                User newUser = new User();
                newUser.setFirstName("Hibernating");
                newUser.setLastName("Rhinos");
                newUser.setLastLogin(new Date());

                Date newDate = DateUtils.setYears(RavenTestHelper.utcToday(), 1993);

                session.advanced().addOrPatch(id, newUser, "lastLogin", newDate);
                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                session.delete(id);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User newUser = new User();
                newUser.setFirstName("Hibernating");
                newUser.setLastName("Rhinos");
                newUser.setLastLogin(new Date(0));

                Date newDate = DateUtils.setYears(RavenTestHelper.utcToday(), 1993);

                session.advanced().addOrPatch(id, newUser, "lastLogin", newDate);
                session.saveChanges();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                User user = session.load(User.class, id);
                assertThat(user.getFirstName())
                        .isEqualTo("Hibernating");
                assertThat(user.getLastName())
                        .isEqualTo("Rhinos");
                assertThat(user.getLastLogin())
                        .isEqualTo(new Date(0));
            }
        }
    }

    @Test
    public void canAddOrPatchAddItemToAnExistingArray() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = "users/1";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setFirstName("Hibernating");
                user.setLastName("Rhinos");

                Date d2000 = DateUtils.setYears(RavenTestHelper.utcToday(), 2000);

                user.setLoginTimes(Collections.singletonList(d2000));
                session.store(user, id);
                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }

            try (IDocumentSession session = store.openSession()) {
                User newUser = new User();
                newUser.setFirstName("Hibernating");
                newUser.setLastName("Rhinos");
                newUser.setLoginTimes(Collections.singletonList(new Date()));

                Date d1993 = DateUtils.setYears(RavenTestHelper.utcToday(), 1993);
                Date d2000 = DateUtils.setYears(RavenTestHelper.utcToday(), 2000);

                session.advanced().addOrPatchArray(id, newUser, "loginTimes", u -> u.add(d1993, d2000));

                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                User user = session.load(User.class, id);
                assertThat(user.getLoginTimes())
                        .hasSize(3);

                session.delete(id);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Date now = new Date();

                User newUser = new User();
                newUser.setLastName("Hibernating");
                newUser.setFirstName("Rhinos");
                newUser.setLastLogin(now);

                Date d1993 = DateUtils.setYears(RavenTestHelper.utcToday(), 1993);

                session
                        .advanced()
                        .addOrPatch(id, newUser, "lastLogin", d1993);

                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                User user = session.load(User.class, id);
                assertThat(user.getLastName())
                        .isEqualTo("Hibernating");
                assertThat(user.getFirstName())
                        .isEqualTo("Rhinos");
                assertThat(user.getLastLogin())
                        .isEqualTo(now);
            }
        }
    }

    @Test
    public void canAddOrPatchIncrement() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = "users/1";

            try (IDocumentSession session = store.openSession()) {
                User newUser = new User();
                newUser.setFirstName("Hibernating");
                newUser.setLastName("Rhinos");
                newUser.setLoginCount(1);

                session.store(newUser, id);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User newUser = new User();
                newUser.setFirstName("Hibernating");
                newUser.setLastName("Rhinos");
                session.advanced().addOrIncrement(id, newUser, "loginCount", 3);

                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                User user = session.load(User.class, id);
                assertThat(user.getLoginCount())
                        .isEqualTo(4);

                session.delete(id);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User newUser = new User();
                newUser.setFirstName("Hibernating");
                newUser.setLastName("Rhinos");
                newUser.setLastLogin(new Date(0));

                Date d1993 = DateUtils.setYears(RavenTestHelper.utcToday(), 1993);

                session.advanced().addOrPatch(id, newUser, "lastLogin", d1993);

                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                User user = session.load(User.class, id);
                assertThat(user.getFirstName())
                        .isEqualTo("Hibernating");
                assertThat(user.getLastName())
                        .isEqualTo("Rhinos");
                assertThat(user.getLastLogin())
                        .isEqualTo(new Date(0));
            }
        }
    }

    public static class User {
        private Date lastLogin;
        private String firstName;
        private String lastName;
        private List<Date> loginTimes;
        private int loginCount;

        public Date getLastLogin() {
            return lastLogin;
        }

        public void setLastLogin(Date lastLogin) {
            this.lastLogin = lastLogin;
        }

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

        public List<Date> getLoginTimes() {
            return loginTimes;
        }

        public void setLoginTimes(List<Date> loginTimes) {
            this.loginTimes = loginTimes;
        }

        public int getLoginCount() {
            return loginCount;
        }

        public void setLoginCount(int loginCount) {
            this.loginCount = loginCount;
        }
    }
}
