package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.session.IDocumentSession;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomEntityNameTest extends RemoteTestBase {

    private char c;

    public static class User {
        private String id;
        private String carId;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCarId() {
            return carId;
        }

        public void setCarId(String carId) {
            this.carId = carId;
        }
    }

    public static class Car {
        private String id;
        private String manufacturer;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }
    }

    private static char[] getChars() {
        Character[] basicChars = IntStream.range(1, 31)
                .mapToObj(x -> (char) x)
                .toArray(Character[]::new);


        char[] extraChars = { 'a', '-', '\'', '\"', '\\', '\b', '\f', '\n', '\r', '\t' };

        return ArrayUtils.addAll(ArrayUtils.toPrimitive(basicChars), extraChars);
    }

    private static char[] getCharactersToTestWithSpecial() {
        char[] basicChars = getChars();
        char[] specialChars = { 'Ā', 'Ȁ', 'Ѐ', 'Ԁ', '؀', '܀', 'ऀ', 'ਅ', 'ଈ', 'అ', 'ഊ', 'ข', 'ဉ', 'ᄍ', 'ሎ', 'ጇ', 'ᐌ', 'ᔎ', 'ᘀ', 'ᜩ', 'ᢹ', 'ᥤ', 'ᨇ' };
        return ArrayUtils.addAll(basicChars, specialChars);
    }

    @Test
    public void findCollectionName() throws Exception {
        for (char c : getCharactersToTestWithSpecial()) {
            testWhenCollectionAndIdContainSpecialChars(c);
        }
    }

    @Override
    protected void customizeStore(DocumentStore store) {
        store.getConventions().setFindCollectionName(clazz -> "Test" + c + DocumentConventions.defaultGetCollectionName(clazz));
    }

    private void testWhenCollectionAndIdContainSpecialChars(char c) throws Exception {
        //TODO RavenDB-15533
        if (c >= 14 && c <= 31) {
            return;
        }

        this.c = c;

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Car car = new Car();
                car.setManufacturer("BMW");
                session.store(car);
                User user = new User();
                user.setCarId(car.getId());
                session.store(user);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<User> results = session.query(User.class, Query.collection(store.getConventions().getFindCollectionName().apply(User.class)))
                        .toList();
                assertThat(results)
                        .hasSize(1);
            }
        }
    }

}
