package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.infrastructure.orders.Address;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14005Test extends RemoteTestBase {

    @Test
    public void canGetCompareExchangeValuesLazilyNoTracking() throws Exception {
        canGetCompareExchangeValuesLazily(true);
    }

    @Test
    public void canGetCompareExchangeValuesLazilyWithTracking() throws Exception {
        canGetCompareExchangeValuesLazily(false);
    }

    public void canGetCompareExchangeValuesLazily(boolean noTracking) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setNoTracking(noTracking);
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Lazy<CompareExchangeValue<Address>> lazyValue =
                        session.advanced().clusterTransaction().lazily().getCompareExchangeValue(Address.class, "companies/hr");

                assertThat(lazyValue.isValueCreated())
                        .isFalse();

                CompareExchangeValue<Address> address = lazyValue.getValue();
                assertThat(address)
                        .isNull();

                CompareExchangeValue<Address> value = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, "companies/hr");
                assertThat(value)
                        .isEqualTo(address);

                Lazy<Map<String, CompareExchangeValue<Address>>> lazyValues = session.advanced()
                        .clusterTransaction().lazily()
                        .getCompareExchangeValues(Address.class, new String[]{"companies/hr", "companies/cf"});

                assertThat(lazyValues.isValueCreated())
                        .isFalse();

                Map<String, CompareExchangeValue<Address>> addresses = lazyValues.getValue();

                assertThat(addresses)
                        .isNotNull()
                        .hasSize(2)
                        .containsKey("companies/hr")
                        .containsKey("companies/cf");

                assertThat(addresses.get("companies/hr"))
                        .isNull();
                assertThat(addresses.get("companies/cf"))
                        .isNull();

                Map<String, CompareExchangeValue<Address>> values = session.advanced().clusterTransaction()
                        .getCompareExchangeValues(Address.class, new String[]{"companies/hr", "companies/cf"});

                assertThat(values)
                        .containsKey("companies/hr")
                        .containsKey("companies/cf");

                assertThat(addresses.get("companies/hr"))
                        .isEqualTo(values.get("companies/hr"));
                assertThat(addresses.get("companies/cf"))
                        .isEqualTo(values.get("companies/cf"));
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Lazy<CompareExchangeValue<Address>> lazyValue = session.advanced()
                        .clusterTransaction()
                        .lazily()
                        .getCompareExchangeValue(Address.class, "companies/hr");
                Lazy<Map<String, CompareExchangeValue<Address>>> lazyValues = session.advanced()
                        .clusterTransaction()
                        .lazily()
                        .getCompareExchangeValues(Address.class, new String[]{"companies/hr", "companies/cf"});

                assertThat(lazyValue.isValueCreated())
                        .isFalse();
                assertThat(lazyValues.isValueCreated())
                        .isFalse();

                session.advanced().eagerly().executeAllPendingLazyOperations();

                int numberOfRequests = session.advanced().getNumberOfRequests();

                CompareExchangeValue<Address> address = lazyValue.getValue();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests);

                assertThat(address)
                        .isNull();

                Map<String, CompareExchangeValue<Address>> addresses = lazyValues.getValue();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequests);

                assertThat(addresses)
                        .isNotNull()
                        .hasSize(2)
                        .containsKey("companies/hr")
                        .containsKey("companies/cf");

                assertThat(addresses.get("companies/hr"))
                        .isNull();
                assertThat(addresses.get("companies/cf"))
                        .isNull();
            }

            SessionOptions clusterSessionOptions = new SessionOptions();
            clusterSessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(clusterSessionOptions)) {
                Address address1 = new Address();
                address1.setCity("Hadera");
                session.advanced().clusterTransaction().createCompareExchangeValue("companies/hr", address1);

                Address address2 = new Address();
                address2.setCity("Torun");
                session.advanced().clusterTransaction().createCompareExchangeValue("companies/cf", address2);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Lazy<CompareExchangeValue<Address>> lazyValue = session
                        .advanced()
                        .clusterTransaction()
                        .lazily()
                        .getCompareExchangeValue(Address.class, "companies/hr");

                assertThat(lazyValue.isValueCreated())
                        .isFalse();

                CompareExchangeValue<Address> address = lazyValue.getValue();
                assertThat(address)
                        .isNotNull();

                assertThat(address.getValue().getCity())
                        .isEqualTo("Hadera");

                CompareExchangeValue<Address> value = session.advanced().clusterTransaction().getCompareExchangeValue(Address.class, "companies/hr");
                assertThat(value.getValue().getCity())
                        .isEqualTo(address.getValue().getCity());

                Lazy<Map<String, CompareExchangeValue<Address>>> lazyValues = session
                        .advanced()
                        .clusterTransaction()
                        .lazily()
                        .getCompareExchangeValues(Address.class, new String[]{"companies/hr", "companies/cf"});

                assertThat(lazyValues.isValueCreated())
                        .isFalse();

                Map<String, CompareExchangeValue<Address>> addresses = lazyValues.getValue();

                assertThat(addresses)
                        .isNotNull()
                        .hasSize(2)
                        .containsKey("companies/hr")
                        .containsKey("companies/cf");

                assertThat(addresses.get("companies/hr").getValue().getCity())
                        .isEqualTo("Hadera");
                assertThat(addresses.get("companies/cf").getValue().getCity())
                        .isEqualTo("Torun");

                Map<String, CompareExchangeValue<Address>> values = session
                        .advanced()
                        .clusterTransaction()
                        .getCompareExchangeValues(Address.class, new String[]{"companies/hr", "companies/cf"});

                assertThat(values)
                        .containsKey("companies/hr")
                        .containsKey("companies/cf");

                assertThat(addresses.get("companies/hr").getValue().getCity())
                        .isEqualTo(values.get("companies/hr").getValue().getCity());
                assertThat(addresses.get("companies/cf").getValue().getCity())
                        .isEqualTo(values.get("companies/cf").getValue().getCity());
            }

        }
    }
}
