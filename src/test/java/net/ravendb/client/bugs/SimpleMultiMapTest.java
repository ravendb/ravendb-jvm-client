package net.ravendb.client.bugs;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractMultiMapIndexCreationTask;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.indexes.GetIndexOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleMultiMapTest extends RemoteTestBase {

    @Test
    public void canCreateMultiMapIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new CatsAndDogs().execute(store);

            IndexDefinition indexDefinition = store.maintenance().send(new GetIndexOperation("CatsAndDogs"));
            assertThat(indexDefinition.getMaps())
                    .hasSize(2);

        }
    }

    @Test
    public void canQueryUsingMultiMap() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new CatsAndDogs().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Cat cat = new Cat();
                cat.setName("Tom");

                Dog dog = new Dog();
                dog.setName("Oscar");

                session.store(cat);
                session.store(dog);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<IHaveName> haveNames = session.query(IHaveName.class, CatsAndDogs.class)
                        .waitForNonStaleResults(Duration.ofSeconds(10))
                        .orderBy("name")
                        .toList();

                assertThat(haveNames)
                        .hasSize(2);

                assertThat(haveNames.get(0))
                        .isInstanceOf(Dog.class);
                assertThat(haveNames.get(1))
                        .isInstanceOf(Cat.class);
            }
        }
    }

    public static class CatsAndDogs extends AbstractMultiMapIndexCreationTask {
        public CatsAndDogs() {
            addMap("from cat in docs.Cats select new { cat.name }");
            addMap("from dog in docs.Dogs select new { dog.name }");
        }
    }

    public interface IHaveName {
        String getName();
    }

    public static class Cat implements IHaveName {
        private String name;

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Dog implements IHaveName {
        private String name;

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
