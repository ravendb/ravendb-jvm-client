package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.queries.SearchOperator;
import net.ravendb.client.documents.session.GroupByField;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.OrderingType;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import static java.util.stream.Collectors.toList;
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
                userCreator.accept("Jane", Arrays.asList("pascal"));

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
                        .containsAll("favourites", Arrays.asList("java"))
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
