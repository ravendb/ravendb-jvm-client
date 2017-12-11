package net.ravendb.client.test.client.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.FieldIndexing;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_5669Test extends RemoteTestBase {

    @Test
    public void workingTestWithDifferentSearchTermOrder() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.executeIndex(new Animal_Index());

            storeAnimals((DocumentStore) store);

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Animal> query = session.advanced().documentQuery(Animal.class, Animal_Index.class);

                query.openSubclause();

                query = query.whereEquals("type", "Cat");
                query = query.orElse();
                query = query.search("name", "Peter*");
                query = query.andAlso();
                query = query.search("name", "Pan*");

                query.closeSubclause();

                List<Animal> results = query.toList();
                assertThat(results)
                        .hasSize(1);
            }
        }
    }

    @Test
    public void workingTestWithSubclause() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.executeIndex(new Animal_Index());

            storeAnimals((DocumentStore) store);

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Animal> query = session.advanced().documentQuery(Animal.class, Animal_Index.class);

                query.openSubclause();

                query = query.whereEquals("type", "Cat");
                query = query.orElse();

                query.openSubclause();

                query = query.search("name", "Pan*");
                query = query.andAlso();
                query = query.search("name", "Peter*");
                query = query.closeSubclause();

                query.closeSubclause();

                List<Animal> results = query.toList();
                assertThat(results)
                        .hasSize(1);
            }
        }
    }

    private void storeAnimals(DocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            Animal animal1 = new Animal();
            animal1.setName("Peter Pan");
            animal1.setType("Dog");

            Animal animal2 = new Animal();
            animal2.setName("Peter Poo");
            animal2.setType("Dog");


            Animal animal3 = new Animal();
            animal3.setName("Peter Foo");
            animal3.setType("Dog");

            session.store(animal1);
            session.store(animal2);
            session.store(animal3);
            session.saveChanges();
        }

        waitForIndexing(store, store.getDatabase());
    }

    public static class Animal {
        private String type;
        private String name;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Animal_Index extends AbstractIndexCreationTask {
        public Animal_Index() {
            map = "from animal in docs.Animals select new { name = animal.name, type = animal.type }";

            analyze("name", "StandardAnalyzer");
            index("name", FieldIndexing.SEARCH);
        }
    }
}
