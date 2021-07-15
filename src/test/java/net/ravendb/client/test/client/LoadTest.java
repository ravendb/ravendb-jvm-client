package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.GeekPerson;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadTest extends RemoteTestBase {

    @Test
    public void loadCanUseCache() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("RavenDB");

                session.store(user, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                User user = newSession.load(User.class, "users/1");
                assertThat(user)
                        .isNotNull();
            }

            try (IDocumentSession newSession = store.openSession()) {
                User user = newSession.load(User.class, "users/1");
                assertThat(user)
                        .isNotNull();
            }
        }
    }

    @Test
    public void load_Document_And_Expect_Null_User() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                String nullId = null;
                User user1 = session.load(User.class, nullId);
                assertThat(user1)
                        .isNull();

                User user2 = session.load(User.class, "");
                assertThat(user2)
                        .isNull();

                User user3 = session.load(User.class, " ");
                assertThat(user3)
                        .isNull();
            }
        }
    }

    @Test
    public void loadDocumentById() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("RavenDB");

                session.store(user, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                User user = newSession.load(User.class, "users/1");
                assertThat(user)
                        .isNotNull();

                assertThat(user.getName())
                        .isEqualTo("RavenDB");
            }
        }
    }

    @Test
    public void loadDocumentsByIds() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("RavenDB");

                User user2 = new User();
                user2.setName("Hibernating Rhinos");

                session.store(user1, "users/1");
                session.store(user2, "users/2");
                session.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                Map<String, User> user = newSession.load(User.class, "users/1", "users/2");
                assertThat(user)
                        .hasSize(2);
            }
        }
    }

    @Test
    public void loadNullShouldReturnNull() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Tony Montana");

                User user2 = new User();
                user2.setName("Tony Soprano");

                session.store(user1);
                session.store(user2);
                session.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                User user1 = newSession.load(User.class, (String) null);
                assertThat(user1)
                        .isNull();
            }
        }
    }

    @Test
    public void loadMultiIdsWithNullShouldReturnDictionaryWithoutNulls() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Tony Montana");

                User user2 = new User();
                user2.setName("Tony Soprano");

                session.store(user1, "users/1");
                session.store(user2, "users/2");
                session.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                String[] orderedArrayOfIdsWithNull = new String[] { "users/1", null, "users/2", null };

                Map<String, User> users1 = newSession.load(User.class, orderedArrayOfIdsWithNull);

                User user1 = users1.get("users/1");
                User user2 = users1.get("users/2");

                assertThat(user1)
                        .isNotNull();

                assertThat(user2)
                        .isNotNull();

                Set<String> unorderedSetOfIdsWithNull = new HashSet<>(Arrays.asList(orderedArrayOfIdsWithNull));
                Map<String, User> users2 = newSession.load(User.class, unorderedSetOfIdsWithNull);

                assertThat(users2)
                        .hasSize(2);
            }
        }
    }

    @Test
    public void loadDocumentWithINtArrayAndLongArray() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                GeekPerson geek1 = new GeekPerson();
                geek1.setName("Bebop");
                geek1.setFavoritePrimes(new int[]{ 13, 43, 443, 997 });
                geek1.setFavoriteVeryLargePrimes(new long[] { 5000000029L, 5000000039L });

                session.store(geek1, "geeks/1");

                GeekPerson geek2 = new GeekPerson();
                geek2.setName("Rocksteady");
                geek2.setFavoritePrimes(new int[] { 2, 3, 5, 7 });
                geek2.setFavoriteVeryLargePrimes(new long [] { 999999999989L });

                session.store(geek2, "geeks/2");
                session.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                GeekPerson geek1 = newSession.load(GeekPerson.class, "geeks/1");
                GeekPerson geek2 = newSession.load(GeekPerson.class, "geeks/2");

                assertThat(geek1.getFavoritePrimes()[1])
                        .isEqualTo(43);
                assertThat(geek1.getFavoriteVeryLargePrimes()[1])
                        .isEqualTo(5000000039L);

                assertThat(geek2.getFavoritePrimes()[3])
                        .isEqualTo(7);
                assertThat(geek2.getFavoriteVeryLargePrimes()[0])
                        .isEqualTo(999999999989L);

            }
        }
    }

    @Test
    public void shouldLoadManyIdsAsPostRequest() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            List<String> ids = new ArrayList<>();

            try (IDocumentSession session = store.openSession()) {
                // Length of all the ids together should be larger than 1024 for POST request
                for (int i = 0; i < 200; i++) {
                    String id = "users/" + i;
                    ids.add(id);

                    User user = new User();
                    user.setName("Person " + i);
                    session.store(user, id);
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, User> users = session.load(User.class, ids);

                User user77 = users.get("users/77");

                assertThat(user77)
                        .isNotNull();

                assertThat(user77.getId())
                        .isEqualTo("users/77");
            }
        }
    }

    @Test
    public void loadStartsWith() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                Consumer<String> createUser = id -> {
                    User u = new User();
                    u.setId(id);
                    session.store(u);
                };

                createUser.accept("Aaa");
                createUser.accept("Abc");
                createUser.accept("Afa");
                createUser.accept("Ala");
                createUser.accept("Baa");

                session.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {

                User[] users = newSession.advanced().loadStartingWith(User.class, "A");

                assertThat(Arrays.stream(users)
                        .map(User::getId)
                        .collect(Collectors.toList()))
                        .containsSequence("Aaa", "Abc", "Afa", "Ala");

                users = newSession.advanced().loadStartingWith(User.class, "A", null, 1, 2);

                assertThat(Arrays.stream(users)
                        .map(User::getId)
                        .collect(Collectors.toList()))
                        .containsSequence("Abc", "Afa");

            }
        }
    }
}
