package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.compareExchange.*;
import net.ravendb.client.documents.session.DocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UniqueValuesTest extends RemoteTestBase {

    @Test
    public void canReadNotExistingKey() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            CompareExchangeValue<Integer> res = store.operations().send(new GetCompareExchangeValueOperation<>(Integer.class, "test"));
            assertThat(res)
                    .isNull();
        }
    }

    @Test
    public void canWorkWithPrimitiveTypes() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            CompareExchangeValue<Integer> res = store.operations().send(new GetCompareExchangeValueOperation<>(Integer.class, "test"));
            assertThat(res)
                    .isNull();

            store.operations().send(new PutCompareExchangeValueOperation<>("test", 5, 0));

            res = store.operations().send(new GetCompareExchangeValueOperation<>(Integer.class, "test"));

            assertThat(res)
                    .isNotNull();
            assertThat(res.getValue())
                    .isEqualTo(5);
        }
    }

    @Test
    public void canPutUniqueString() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (DocumentSession session = (DocumentSession) store.openSession()) {
                store.operations().send(new PutCompareExchangeValueOperation<>("test", "Karmel", 0));
                CompareExchangeValue<String> res = store.operations().send(new GetCompareExchangeValueOperation<>(String.class, "test"));
                assertThat(res.getValue())
                        .isEqualTo("Karmel");
            }
        }
    }

    @Test
    public void canPutMultiDifferentValues() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user1 = new User();
            user1.setName("Karmel");

            CompareExchangeResult<User> res = store.operations().send(new PutCompareExchangeValueOperation<>("test", user1, 0));

            User user2 = new User();
            user2.setName("Karmel");

            CompareExchangeResult<User> res2 = store.operations().send(new PutCompareExchangeValueOperation<>("test2", user2, 0));


            assertThat(res.getValue().getName())
                    .isEqualTo("Karmel");
            assertThat(res.isSuccessful())
                    .isTrue();

            assertThat(res2.getValue().getName())
                    .isEqualTo("Karmel");
            assertThat(res2.isSuccessful())
                    .isTrue();
        }
    }

    @Test
    public void canListCompareExchange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user1 = new User();
            user1.setName("Karmel");
            CompareExchangeResult<User> res1 = store.operations().send(new PutCompareExchangeValueOperation<>("test", user1, 0));

            User user2 = new User();
            user2.setName("Karmel");
            CompareExchangeResult<User> res2 = store.operations().send(new PutCompareExchangeValueOperation<>("test2", user2, 0));

            assertThat(res1.getValue().getName())
                    .isEqualTo("Karmel");

            assertThat(res1.isSuccessful())
                    .isTrue();

            assertThat(res2.getValue().getName())
                    .isEqualTo("Karmel");

            assertThat(res2.isSuccessful())
                    .isTrue();

            Map<String, CompareExchangeValue<User>> values = store.operations().send(new GetCompareExchangeValuesOperation<>(User.class, "test"));
            assertThat(values)
                    .hasSize(2);

            assertThat(values.get("test").getValue().getName())
                    .isEqualTo("Karmel");

            assertThat(values.get("test2").getValue().getName())
                    .isEqualTo("Karmel");
        }
    }

    @Test
    public void canRemoveUnique() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            CompareExchangeResult<String> res = store.operations().send(new PutCompareExchangeValueOperation<>("test", "Karmel", 0));

            assertThat(res.getValue())
                    .isEqualTo("Karmel");

            assertThat(res.isSuccessful())
                    .isTrue();

            res = store.operations().send(new DeleteCompareExchangeValueOperation<>(String.class,"test", res.getIndex()));
            assertThat(res.isSuccessful())
                    .isTrue();

        }
    }

    @Test
    public void removeUniqueFailed() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            CompareExchangeResult<String> res = store.operations().send(new PutCompareExchangeValueOperation<>("test", "Karmel", 0));
            assertThat(res.getValue())
                    .isEqualTo("Karmel");

            assertThat(res.isSuccessful())
                    .isTrue();

            res = store.operations().send(new DeleteCompareExchangeValueOperation<>(String.class, "test", 0));
            assertThat(res.isSuccessful())
                    .isFalse();

            CompareExchangeValue<String> readValue = store.operations().send(new GetCompareExchangeValueOperation<>(String.class, "test"));
            assertThat(readValue.getValue())
                    .isEqualTo("Karmel");
        }
    }

    @Test
    public void returnCurrentValueWhenPuttingConcurrently() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user = new User();
            user.setName("Karmel");

            User user2 = new User();
            user2.setName("Karmel2");

            CompareExchangeResult<User> res = store.operations().send(new PutCompareExchangeValueOperation<>("test", user, 0));
            CompareExchangeResult<User> res2 = store.operations().send(new PutCompareExchangeValueOperation<>("test", user2, 0));

            assertThat(res.isSuccessful())
                    .isTrue();
            assertThat(res2.isSuccessful())
                    .isFalse();
            assertThat(res.getValue().getName())
                    .isEqualTo("Karmel");
            assertThat(res2.getValue().getName())
                    .isEqualTo("Karmel");

            User user3 = new User();
            user3.setName("Karmel2");

            res2 = store.operations().send(new PutCompareExchangeValueOperation<>("test", user3, res2.getIndex()));
            assertThat(res2.isSuccessful())
                    .isTrue();

            assertThat(res2.getValue().getName())
                    .isEqualTo("Karmel2");
        }
    }

    @Test
    public void canGetIndexValue() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user = new User();
            user.setName("Karmel");

            store.operations().send(new PutCompareExchangeValueOperation<>("test", user, 0));
            CompareExchangeValue<User> res = store.operations().send(new GetCompareExchangeValueOperation<>(User.class, "test"));

            assertThat(res.getValue().getName())
                    .isEqualTo("Karmel");

            User user2 = new User();
            user2.setName("Karmel2");

            CompareExchangeResult<User> res2 = store.operations().send(new PutCompareExchangeValueOperation<>("test", user2, res.getIndex()));
            assertThat(res2.isSuccessful())
                    .isTrue();

            assertThat(res2.getValue().getName())
                    .isEqualTo("Karmel2");
        }
    }
}
