package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

public class ContainsTest extends RemoteTestBase {

    @Test
    public void containsTest() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                BiConsumer<String, List<String>> userCreator = (name, favs) -> {
                    UserWithFavs user = new UserWithFavs();
                    user.setName(name);
                    user.setFavourites(favs);

                    session.store(user);
                };

                userCreator.accept("John", Arrays.asList("java", "c#"));
                userCreator.accept("Tarzan", Arrays.asList("java", "go"));
                userCreator.accept("Jane", Collections.singletonList("pascal"));

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<String> pascalOrGoDeveloperNames = session
                        .query(UserWithFavs.class)
                        .containsAny("favourites", Arrays.asList("pascal", "go"))
                        .selectFields(String.class, "name")
                        .toList();

                assertThat(pascalOrGoDeveloperNames)
                        .hasSize(2)
                        .contains("Jane")
                        .contains("Tarzan");
            }

            try (IDocumentSession session = store.openSession()) {
                List<String> javaDevelopers = session
                        .query(UserWithFavs.class)
                        .containsAll("favourites", Collections.singletonList("java"))
                        .selectFields(String.class, "name")
                        .toList();

                assertThat(javaDevelopers)
                        .hasSize(2)
                        .contains("John")
                        .contains("Tarzan");
            }
        }
    }


    public static class UserWithFavs {
        private String id;
        private String name;
        private List<String> favourites;

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

        public List<String> getFavourites() {
            return favourites;
        }

        public void setFavourites(List<String> favourites) {
            this.favourites = favourites;
        }
    }



}
