package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadTest extends RemoteTestBase {

    @Test
    public void loadDocumentById() throws IOException {
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
    public void loadDocumentsByIds() throws IOException {
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
    public void loadNullShouldReturnNull() throws IOException {
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

    /* TODO


        [Fact]
        public void Load_Multi_Ids_With_Null_Should_Return_Dictionary_Without_nulls()
        {
            using (var store = GetDocumentStore())
            {
                using (var session = store.OpenSession())
                {
                    session.Store(new User { Name = "Tony Montana" }, "users/1");
                    session.Store(new User { Name = "Tony Soprano" }, "users/2");
                    session.SaveChanges();
                }

                using (var newSession = store.OpenSession())
                {
                    var orderedArrayOfIdsWithNull = new[] { "users/1", null, "users/2", null };
                    var users1 = newSession.Load<User>(orderedArrayOfIdsWithNull);
                    User user1;
                    User user2;
                    users1.TryGetValue("users/1", out user1);
                    users1.TryGetValue("users/2", out user2);

                    Assert.NotNull(user1);
                    Assert.NotNull(user2);

                    var unorderedSetOfIdsWithNull = new HashSet<string>() { "users/1", null, "users/2", null };
                    var users2 = newSession.Load<User>(unorderedSetOfIdsWithNull);
                    Assert.Equal(users2.Count, 2);
                }
            }
        }

        [Fact]
        public void Load_Document_With_Int_Array_And_Long_Array()
        {
            using (var store = GetDocumentStore())
            {
                using (var session = store.OpenSession())
                {
                    session.Store(new GeekPerson { Name = "Bebop", FavoritePrimes = new[] { 13, 43, 443, 997 }, FavoriteVeryLargePrimes = new[] { 5000000029, 5000000039 } }, "geeks/1");
                    session.Store(new GeekPerson { Name = "Rocksteady", FavoritePrimes = new[] { 2, 3, 5, 7 }, FavoriteVeryLargePrimes = new[] { 999999999989 } }, "geeks/2");
                    session.SaveChanges();
                }

                using (var newSession = store.OpenSession())
                {
                    var geek1 = newSession.Load<GeekPerson>("geeks/1");
                    var geek2 = newSession.Load<GeekPerson>("geeks/2");

                    Assert.Equal(geek1.FavoritePrimes[1], 43);
                    Assert.Equal(geek1.FavoriteVeryLargePrimes[1], 5000000039);

                    Assert.Equal(geek2.FavoritePrimes[3], 7);
                    Assert.Equal(geek2.FavoriteVeryLargePrimes[0], 999999999989);
                }
            }
        }

        [Fact]
        public void Should_Load_Many_Ids_As_Post_Request()
        {
            using (var store = GetDocumentStore())
            {
                var ids = new List<string>();
                using (var session = store.OpenSession())
                {
                    // Length of all the ids together should be larger than 1024 for POST request
                    for (int i = 0; i < 200; i++)
                    {
                        var id = "users/" + i;
                        ids.Add(id);

                        session.Store(new User()
                        {
                            Name = "Person " + i
                        }, id);
                    }
                    session.SaveChanges();
                }

                using (var session = store.OpenSession())
                {
                    var users = session.Load<User>(ids);
                    User user77;
                    users.TryGetValue("users/77", out user77);
                    Assert.NotNull(user77);
                    Assert.Equal(user77.Id, "users/77");
                }
            }
        }
     */
}
