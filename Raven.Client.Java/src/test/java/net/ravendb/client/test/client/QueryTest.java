package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.OrderingType;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.CleanCloseable;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryTest extends RemoteTestBase {

    @Test
    public void querySimple() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                User user1 = new User();
                user1.setName("John");

                User user2 = new User();
                user2.setName("Jane");

                User user3 = new User();
                user3.setName("Tarzan");

                session.store(user1, "users/1");
                session.store(user2, "users/2");
                session.store(user3, "users/3");
                session.saveChanges();

                List<User> queryResult = session.advanced().documentQuery(User.class, null, "users", false)
                        .toList();

                assertThat(queryResult)
                        .hasSize(3);
            }
        }
    }

    @Test
    public void queryWithWhereClause() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                User user1 = new User();
                user1.setName("John");

                User user2 = new User();
                user2.setName("Jane");

                User user3 = new User();
                user3.setName("Tarzan");

                session.store(user1, "users/1");
                session.store(user2, "users/2");
                session.store(user3, "users/3");
                session.saveChanges();

                List<User> queryResult = session.advanced().documentQuery(User.class, null, "users", false)
                        .whereStartsWith("Name", "J")
                        .toList();

                List<User> queryResult2 = session.advanced().documentQuery(User.class, null, "users", false)
                        .whereEquals("Name", "Tarzan")
                        .toList();

                List<User> queryResult3 = session.advanced().documentQuery(User.class, null, "users", false)
                        .whereEndsWith("Name", "n")
                        .toList();

                assertThat(queryResult)
                        .hasSize(2);

                assertThat(queryResult2)
                        .hasSize(1);

                assertThat(queryResult3)
                        .hasSize(2);
            }
        }
    }

    @Test
    public void queryWithCustomize() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {

            new DogsIndex().execute(store);

            try (IDocumentSession newSession = store.openSession()) {
                createDogs(newSession);

                newSession.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {

                List<DogsIndex.Result> queryResult = newSession.advanced()
                        .documentQuery(DogsIndex.Result.class, new DogsIndex().getIndexName(), null, false)
                        .waitForNonStaleResults()
                        .orderBy("Name", OrderingType.ALPHA_NUMERIC)
                        .whereGreaterThan("Age", 2)
                        .toList();

                assertThat(queryResult)
                        .hasSize(4);

                assertThat(queryResult.get(0).getName())
                        .isEqualTo("Brian");

                assertThat(queryResult.get(1).getName())
                        .isEqualTo("Django");

                assertThat(queryResult.get(2).getName())
                        .isEqualTo("Lassie");

                assertThat(queryResult.get(3).getName())
                        .isEqualTo("Snoopy");
            }
        }
    }

    private void createDogs(IDocumentSession newSession) {
        Dog dog1 = new Dog();
        dog1.setName("Snoopy");
        dog1.setBreed("Beagle");
        dog1.setColor("White");
        dog1.setAge(6);
        dog1.setVaccinated(true);

        newSession.store(dog1, "docs/1");

        Dog dog2 = new Dog();
        dog2.setName("Brian");
        dog2.setBreed("Labrador");
        dog2.setColor("White");
        dog2.setAge(12);
        dog2.setVaccinated(false);

        newSession.store(dog2, "docs/2");

        Dog dog3 = new Dog();
        dog3.setName("Django");
        dog3.setBreed("Jack Russel");
        dog3.setColor("Black");
        dog3.setAge(3);
        dog3.setVaccinated(true);

        newSession.store(dog3, "docs/3");

        Dog dog4 = new Dog();
        dog4.setName("Beethoven");
        dog4.setBreed("St. Bernard");
        dog4.setColor("Brown");
        dog4.setAge(1);
        dog4.setVaccinated(false);

        newSession.store(dog4, "docs/4");

        Dog dog5 = new Dog();
        dog5.setName("Scooby Doo");
        dog5.setBreed("Great Dane");
        dog5.setColor("Brown");
        dog5.setAge(0);
        dog5.setVaccinated(false);

        newSession.store(dog5, "docs/5");

        Dog dog6 = new Dog();
        dog6.setName("Old Yeller");
        dog6.setBreed("Black Mouth Cur");
        dog6.setColor("White");
        dog6.setAge(2);
        dog6.setVaccinated(true);

        newSession.store(dog6, "docs/6");

        Dog dog7 = new Dog();
        dog7.setName("Benji");
        dog7.setBreed("Mixed");
        dog7.setColor("White");
        dog7.setAge(0);
        dog7.setVaccinated(false);

        newSession.store(dog7, "docs/7");

        Dog dog8 = new Dog();
        dog8.setName("Lassie");
        dog8.setBreed("Collie");
        dog8.setColor("Brown");
        dog8.setAge(6);
        dog8.setVaccinated(true);

        newSession.store(dog8, "docs/8");
    }

    @Test
    public void queryLongRequest() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession newSession = store.openSession()) {
                String longName = StringUtils.repeat('x', 2048);
                User user = new User();
                user.setName(longName);
                newSession.store(user, "users/1");

                newSession.saveChanges();

                List<User> queryResult = newSession
                        .advanced()
                        .documentQuery(User.class, null, "Users", false)
                        .whereEquals("Name", longName)
                        .toList();

                assertThat(queryResult)
                        .hasSize(1);
            }
        }
    }

    @Test
    public void queryByIndex() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {

            new DogsIndex().execute(store);

            try (IDocumentSession newSession = store.openSession()) {
                createDogs(newSession);

                newSession.saveChanges();

                waitForIndexing(store, store.getDatabase(), null);
            }

            try (IDocumentSession newSession = store.openSession()) {
                List<DogsIndex.Result> queryResult = newSession.advanced()
                        .documentQuery(DogsIndex.Result.class, new DogsIndex().getIndexName(), null, false)
                        .whereGreaterThan("Age", 2)
                        .andAlso()
                        .whereEquals("Vaccinated", false)
                        .toList();

                assertThat(queryResult)
                        .hasSize(1);

                assertThat(queryResult.get(0).getName())
                        .isEqualTo("Brian");


                List<DogsIndex.Result> queryResult2 = newSession.advanced()
                        .documentQuery(DogsIndex.Result.class, new DogsIndex().getIndexName(), null, false)
                        .whereLessThanOrEqual("Age", 2)
                        .andAlso()
                        .whereEquals("Vaccinated", false)
                        .toList();

                assertThat(queryResult2)
                        .hasSize(3);

                List<String> list = queryResult2.stream()
                        .map(x -> x.getName())
                        .collect(toList());

                assertThat(list)
                        .contains("Beethoven")
                        .contains("Scooby Doo")
                        .contains("Benji");
            }
        }
    }

    public static class Dog {
        private String id;
        private String name;
        private String breed;
        private String color;
        private int age;
        private boolean isVaccinated;

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

        public String getBreed() {
            return breed;
        }

        public void setBreed(String breed) {
            this.breed = breed;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public boolean isVaccinated() {
            return isVaccinated;
        }

        public void setVaccinated(boolean vaccinated) {
            isVaccinated = vaccinated;
        }
    }

    public static class DogsIndex extends AbstractIndexCreationTask {
        public static class Result {
            private String name;
            private int age;
            private boolean isVaccinated;

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

            public boolean isVaccinated() {
                return isVaccinated;
            }

            public void setVaccinated(boolean vaccinated) {
                isVaccinated = vaccinated;
            }
        }

        public DogsIndex() {
            map = "from dog in docs.dogs select new { dog.Name, dog.Age, dog.Vaccinated }";
        }
    }
}
