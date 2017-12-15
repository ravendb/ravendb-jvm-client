package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.queries.SearchOperator;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class QueryTest extends RemoteTestBase {

    @Test
    public void querySimple() throws Exception {
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
    public void queryWithWhereClause() throws Exception {
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

                List<User> queryResult = session.query(User.class, Query.collection("users"))
                        .whereStartsWith("name", "J")
                        .toList();

                List<User> queryResult2 = session.query(User.class, Query.collection("users"))
                        .whereEquals("name", "Tarzan")
                        .toList();

                List<User> queryResult3 = session.query(User.class, Query.collection("users"))
                        .whereEndsWith("name", "n")
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
    public void queryMapReduceWithCount() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {
                List<ReduceResult> results = session.query(User.class)
                        .groupBy("name")
                        .selectKey()
                        .selectCount()
                        .orderByDescending("count")
                        .ofType(ReduceResult.class)
                        .toList();

                assertThat(results.get(0).getCount())
                        .isEqualTo(2);
                assertThat(results.get(0).getName())
                        .isEqualTo("John");

                assertThat(results.get(1).getCount())
                        .isEqualTo(1);
                assertThat(results.get(1).getName())
                        .isEqualTo("Tarzan");
            }
        }
    }

    @Test
    public void queryMapReduceWithSum() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {
                List<ReduceResult> results = session.query(User.class)
                        .groupBy("name")
                        .selectKey()
                        .selectSum(new GroupByField("age"))
                        .orderByDescending("age")
                        .ofType(ReduceResult.class)
                        .toList();

                assertThat(results.get(0).getAge())
                        .isEqualTo(8);
                assertThat(results.get(0).getName())
                        .isEqualTo("John");

                assertThat(results.get(1).getAge())
                        .isEqualTo(2);
                assertThat(results.get(1).getName())
                        .isEqualTo("Tarzan");
            }
        }
    }

    @Test
    public void queryMapReduceIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {

                List<ReduceResult> results = session.query(ReduceResult.class, Query.index("UsersByName"))
                        .orderByDescending("count")
                        .toList();

                assertThat(results.get(0).getCount())
                        .isEqualTo(2);
                assertThat(results.get(0).getName())
                        .isEqualTo("John");

                assertThat(results.get(1).getCount())
                        .isEqualTo(1);
                assertThat(results.get(1).getName())
                        .isEqualTo("Tarzan");
            }
        }
    }

    @Test
    public void querySingleProperty() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {

                List<Integer> ages = session.query(User.class)
                        .addOrder("age", true, OrderingType.LONG)
                        .selectFields(Integer.class, "age")
                        .toList();

                assertThat(ages)
                        .hasSize(3)
                        .containsSequence(5, 3, 2);
            }
        }
    }

    @Test
    public void queryWithSelect() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {

                List<User> usersAge = session.query(User.class)
                        .selectFields(User.class, "age")
                        .toList();

                for (User user : usersAge) {
                    assertThat(user.getAge())
                            .isPositive();

                    assertThat(user.getId())
                            .isNotNull();
                }
            }
        }
    }

    @Test
    public void queryWithWhereIn() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {

                List<User> users = session.query(User.class)
                        .whereIn("name", Arrays.asList("Tarzan", "no_such"))
                        .toList();

                assertThat(users)
                        .hasSize(1);
            }
        }
    }

    @Test
    public void queryWithWhereBetween() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {

                List<User> users = session.query(User.class)
                        .whereBetween("age", 4, 5)
                        .toList();

                assertThat(users)
                        .hasSize(1);

                assertThat(users.get(0).getName())
                        .isEqualTo("John");

            }
        }
    }

    @Test
    public void queryWithWhereLessThan() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {

                List<User> users = session.query(User.class)
                        .whereLessThan("age", 3)
                        .toList();

                assertThat(users)
                        .hasSize(1);

                assertThat(users.get(0).getName())
                        .isEqualTo("Tarzan");

            }
        }
    }

    @Test
    public void queryWithWhereLessThanOrEqual() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {

                List<User> users = session.query(User.class)
                        .whereLessThanOrEqual("age", 3)
                        .toList();

                assertThat(users)
                        .hasSize(2);

            }
        }
    }

    @Test
    public void queryWithWhereGreaterThan() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {

                List<User> users = session.query(User.class)
                        .whereGreaterThan("age", 3)
                        .toList();

                assertThat(users)
                        .hasSize(1);

                assertThat(users.get(0).getName())
                        .isEqualTo("John");

            }
        }
    }

    @Test
    public void queryWithWhereGreaterThanOrEqual() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {
                List<User> users = session.query(User.class)
                        .whereGreaterThanOrEqual("age", 3)
                        .toList();

                assertThat(users)
                        .hasSize(2);

            }
        }
    }

    private static class UserProjection {
        private String id;
        private String name;

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
    }

    @Test
    public void queryWithProjection() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {

                List<UserProjection> projections = session.query(User.class)
                        .selectFields(UserProjection.class)
                        .toList();

                assertThat(projections)
                        .hasSize(3);

                for (UserProjection projection : projections) {
                    assertThat(projection.getId())
                            .isNotNull();

                    assertThat(projection.getName())
                            .isNotNull();
                }
            }
        }
    }

    @Test
    public void queryWithProjection2() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {

                List<UserProjection> projections = session.query(User.class)
                        .selectFields(UserProjection.class, "lastName")
                        .toList();

                assertThat(projections)
                        .hasSize(3);

                for (UserProjection projection : projections) {
                    assertThat(projection.getId())
                            .isNotNull();

                    assertThat(projection.getName())
                            .isNull(); // we didn't specify this field in mapping
                }
            }
        }
    }

    @Test
    public void queryDistinct() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {
                List<String> uniqueNames = session.query(User.class)
                        .selectFields(String.class, "name")
                        .distinct()
                        .toList();

                assertThat(uniqueNames)
                        .hasSize(2)
                        .contains("Tarzan")
                        .contains("John");
            }
        }
    }

    @Test
    public void querySearchWithOr() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (IDocumentSession session = store.openSession()) {
                List<User> uniqueNames = session.query(User.class)
                        .search("name", "Tarzan John", SearchOperator.OR)
                        .toList();

                assertThat(uniqueNames)
                        .hasSize(3);
            }
        }
    }

    @Test
    public void queryNoTracking() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (DocumentSession session = (DocumentSession) store.openSession()) {
                List<User> users = session.query(User.class)
                        .noTracking()
                        .toList();

                assertThat(users)
                        .hasSize(3);

                for (User user : users) {
                    assertThat(session.isLoaded(user.getId()))
                            .isFalse();
                }
            }
        }
    }

    @Test
    public void querySkipTake() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (DocumentSession session = (DocumentSession) store.openSession()) {
                List<User> users = session.query(User.class)
                        .orderBy("name")
                        .skip(2)
                        .take(1)
                        .toList();

                assertThat(users)
                        .hasSize(1);

                assertThat(users.get(0).getName())
                        .isEqualTo("Tarzan");
            }
        }
    }

    @Test
    public void queryLucene() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (DocumentSession session = (DocumentSession) store.openSession()) {
                List<User> users = session.query(User.class)
                        .whereLucene("name", "Tarzan")
                        .toList();

                assertThat(users)
                        .hasSize(1);

                for (User user : users) {
                    assertThat(user.getName())
                            .isEqualTo("Tarzan");
                }
            }
        }
    }

    @Test
    public void queryWhereExact() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (DocumentSession session = (DocumentSession) store.openSession()) {
                List<User> users = session.query(User.class)
                        .whereEquals("name", "tarzan")
                        .toList();

                assertThat(users)
                        .hasSize(1);

                users = session.query(User.class)
                        .whereEquals("name", "tarzan", true)
                        .toList();

                assertThat(users)
                        .hasSize(0); // we queried for tarzan with exact

                users = session.query(User.class)
                        .whereEquals("name", "Tarzan", true)
                        .toList();

                assertThat(users)
                        .hasSize(1); // we queried for Tarzan with exact
            }
        }
    }

    @Test
    public void queryWhereNot() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (DocumentSession session = (DocumentSession) store.openSession()) {
                assertThat(session.query(User.class)
                        .not()
                        .whereEquals("name", "tarzan")
                        .toList())
                        .hasSize(2);

                assertThat(session.query(User.class)
                        .whereNotEquals("name", "tarzan")
                        .toList())
                        .hasSize(2);

                assertThat(session.query(User.class)
                        .whereNotEquals("name", "Tarzan", true)
                        .toList())
                        .hasSize(2);
            }
        }
    }

    @Test
    public void queryFirst() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (DocumentSession session = (DocumentSession) store.openSession()) {

                User first = session.query(User.class)
                        .first();

                assertThat(first)
                        .isNotNull();

                assertThat(session.query(User.class)
                        .whereEquals("name", "Tarzan")
                        .single())
                        .isNotNull();

                assertThat(first)
                        .isNotNull();

                assertThatThrownBy(() -> session.query(User.class).single())
                        .isExactlyInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Test
    public void queryParameters() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (DocumentSession session = (DocumentSession) store.openSession()) {

                assertThat(session.rawQuery(User.class, "from Users where name = $name")
                        .addParameter("name", "Tarzan")
                        .count())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void queryRandomOrder() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (DocumentSession session = (DocumentSession) store.openSession()) {

                assertThat(session.query(User.class)
                        .randomOrdering()
                        .toList())
                        .hasSize(3);

                assertThat(session.query(User.class)
                        .randomOrdering("123")
                        .toList())
                        .hasSize(3);
            }
        }
    }

    @Test
    public void queryWhereExists() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (DocumentSession session = (DocumentSession) store.openSession()) {
                assertThat(session.query(User.class)
                        .whereExists("name")
                        .toList())
                        .hasSize(3);

                assertThat(session.query(User.class)
                        .whereExists("name")
                        .andAlso()
                        .not()
                        .whereExists("no_such_field")
                        .toList())
                        .hasSize(3);
            }
        }
    }

    @Test
    public void queryWithBoost() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            try (DocumentSession session = (DocumentSession) store.openSession()) {
                List<User> users = session.query(User.class)
                        .whereEquals("name", "Tarzan")
                        .boost(5)
                        .orElse()
                        .whereEquals("name", "John")
                        .boost(2)
                        .orderByScoreDescending()
                        .toList();

                assertThat(users)
                        .hasSize(3);

                List<String> names = users.stream().map(User::getName).collect(toList());
                assertThat(names)
                        .containsSequence("Tarzan", "John", "John");

                users = session.query(User.class)
                        .whereEquals("name", "Tarzan")
                        .boost(2)
                        .orElse()
                        .whereEquals("name", "John")
                        .boost(5)
                        .orderByScoreDescending()
                        .toList();

                assertThat(users)
                        .hasSize(3);

                names = users.stream().map(x -> x.getName()).collect(toList());
                assertThat(names)
                        .containsSequence("John", "John", "Tarzan");
            }
        }
    }

    public static class UsersByName extends AbstractIndexCreationTask {
        public UsersByName() {

            map = "from c in docs.Users select new " +
                    " {" +
                    "    c.name, " +
                    "    count = 1" +
                    "}";

            reduce = "from result in results " +
                    "group result by result.name " +
                    "into g " +
                    "select new " +
                    "{ " +
                    "  name = g.Key, " +
                    "  count = g.Sum(x => x.count) " +
                    "}";
        }
    }

    private void addUsers(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            User user1 = new User();
            user1.setName("John");
            user1.setAge(3);

            User user2 = new User();
            user2.setName("John");
            user2.setAge(5);

            User user3 = new User();
            user3.setName("Tarzan");
            user3.setAge(2);

            session.store(user1, "users/1");
            session.store(user2, "users/2");
            session.store(user3, "users/3");
            session.saveChanges();
        }

        store.executeIndex(new UsersByName());
        waitForIndexing(store);
    }

    public static class ReduceResult {
        private int count;
        private String name;
        private int age;

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void queryWithCustomize() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            new DogsIndex().execute(store);

            try (IDocumentSession newSession = store.openSession()) {
                createDogs(newSession);

                newSession.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {

                List<DogsIndex.Result> queryResult = newSession.advanced()
                        .documentQuery(DogsIndex.Result.class, new DogsIndex().getIndexName(), null, false)
                        .waitForNonStaleResults(null)
                        .orderBy("name", OrderingType.ALPHA_NUMERIC)
                        .whereGreaterThan("age", 2)
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
    public void queryLongRequest() throws Exception {
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
                        .whereEquals("name", longName)
                        .toList();

                assertThat(queryResult)
                        .hasSize(1);
            }
        }
    }

    @Test
    public void queryByIndex() throws Exception {
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
                        .whereGreaterThan("age", 2)
                        .andAlso()
                        .whereEquals("vaccinated", false)
                        .toList();

                assertThat(queryResult)
                        .hasSize(1);

                assertThat(queryResult.get(0).getName())
                        .isEqualTo("Brian");


                List<DogsIndex.Result> queryResult2 = newSession.advanced()
                        .documentQuery(DogsIndex.Result.class, new DogsIndex().getIndexName(), null, false)
                        .whereLessThanOrEqual("age", 2)
                        .andAlso()
                        .whereEquals("vaccinated", false)
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
            map = "from dog in docs.dogs select new { dog.name, dog.age, dog.vaccinated }";
        }
    }
}
