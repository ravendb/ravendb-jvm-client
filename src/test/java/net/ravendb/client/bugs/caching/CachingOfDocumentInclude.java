package net.ravendb.client.bugs.caching;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.IDocumentSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class CachingOfDocumentInclude extends RemoteTestBase {

    public static class User {
        private String id;
        private String name;
        private String partnerId;
        private String email;
        private String[] tags;
        private int age;
        private boolean active;

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

        public String getPartnerId() {
            return partnerId;
        }

        public void setPartnerId(String partnerId) {
            this.partnerId = partnerId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String[] getTags() {
            return tags;
        }

        public void setTags(String[] tags) {
            this.tags = tags;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

    @Test
    public void can_cache_document_with_includes() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Ayende");
                session.store(user);

                User partner = new User();
                partner.setPartnerId("users/1-A");
                session.store(partner);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.include("partnerId")
                        .load(User.class, "users/2-A");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.include("partnerId")
                        .load(User.class, "users/2-A");

                assertThat(session.advanced().getRequestExecutor().getCache().getNumberOfItems())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void can_avoid_using_server_for_load_with_include_if_everything_is_in_session_cacheAsync() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Ayende");
                session.store(user);

                User partner = new User();
                partner.setPartnerId("users/1-A");
                session.store(partner);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/2-A");

                session.load(User.class, user.getPartnerId());

                int old = session.advanced().getNumberOfRequests();
                User newUser = session.include("partnerId")
                        .load(User.class, "users/2-A");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(old);
            }
        }
    }

    @Test
    public void can_avoid_using_server_for_load_with_include_if_everything_is_in_session_cacheLazy() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Ayende");
                session.store(user);

                User partner = new User();
                partner.setPartnerId("users/1-A");
                session.store(partner);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().lazily().load(User.class, "users/2-A");
                session.advanced().lazily().load(User.class, "users/1-A");
                session.advanced().eagerly().executeAllPendingLazyOperations();

                int old = session.advanced().getNumberOfRequests();

                Lazy<User> result1 = session.advanced().lazily()
                        .include("partnerId")
                        .load(User.class, "users/2-A");

                assertThat(result1.getValue())
                        .isNotNull();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(old);
            }
        }
    }

    @Test
    public void can_avoid_using_server_for_load_with_include_if_everything_is_in_session_cache() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Ayende");
                session.store(user);

                User partner = new User();
                partner.setPartnerId("users/1-A");
                session.store(partner);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/2-A");

                session.load(User.class, user.getPartnerId());

                int old = session.advanced().getNumberOfRequests();

                User res = session.include("partnerId")
                        .load(User.class, "users/2-A");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(old);
            }
        }
    }

    @Test
    public void can_avoid_using_server_for_multiload_with_include_if_everything_is_in_session_cache() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (final IDocumentSession session = store.openSession()) {
                Consumer<String> storeUser = name -> {
                    User user = new User();
                    user.setName(name);
                    session.store(user);
                };

                storeUser.accept("Additional");
                storeUser.accept("Ayende");
                storeUser.accept("Michael");
                storeUser.accept("Fitzhak");
                storeUser.accept("Maxim");

                User withPartner = new User();
                withPartner.setPartnerId("users/1-A");
                session.store(withPartner);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User u2 = session.load(User.class, "users/2-A");
                User u6 = session.load(User.class, "users/6-A");

                ArrayList<String> inp = new ArrayList<>();
                inp.add("users/1-A");
                inp.add("users/2-A");
                inp.add("users/3-A");
                inp.add("users/4-A");
                inp.add("users/5-A");
                inp.add("users/6-A");
                Map<String, User> u4 = session.load(User.class, inp);

                session.load(User.class, u6.getPartnerId());

                int old = session.advanced().getNumberOfRequests();

                Map<String, User> res = session.include("partnerId")
                        .load(User.class, "users/2-A", "users/3-A", "users/6-A");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(old);

            }
        }
    }
}
