package net.ravendb.client.test.client;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetDocumentsCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomSerializationTest extends RemoteTestBase {

    @Test
    public void testSerialization() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Product product1 = new Product();
                product1.setName("iPhone");
                product1.setPrice(Money.forDollars(9999));

                Product product2 = new Product();
                product2.setName("Camera");
                product2.setPrice(Money.forEuro(150));

                Product product3 = new Product();
                product3.setName("Bread");
                product3.setPrice(Money.forDollars(2));

                session.store(product1);
                session.store(product2);
                session.store(product3);
                session.saveChanges();
            }

            // verify if value was properly serialized
            {
                GetDocumentsCommand command = new GetDocumentsCommand("products/1-A", null, false);
                store.getRequestExecutor().execute(command);
                JsonNode productJson = command.getResult().getResults().get(0);

                JsonNode priceNode = productJson.get("price");
                assertThat(priceNode.asText())
                        .isEqualTo("9999 USD");
            }

            //verify if query properly serialize value
            {
                try (IDocumentSession session = store.openSession()) {
                    List<Product> productsForTwoDollars = session.query(Product.class)
                            .whereEquals("price", Money.forDollars(2))
                            .toList();

                    assertThat(productsForTwoDollars)
                            .hasSize(1);

                    assertThat(productsForTwoDollars.get(0).getName())
                            .isEqualTo("Bread");
                }
            }

        }
    }


    @Override
    protected void customizeStore(DocumentStore store) {
        DocumentConventions conventions = store.getConventions();

        SimpleModule module = new SimpleModule();
        module.addSerializer(new MoneySerializer());
        module.addDeserializer(Money.class, new MoneyDeserializer());

        conventions.getEntityMapper().registerModule(module);

        conventions.registerQueryValueConverter(Money.class, (fieldName, value, forRange, stringValue) -> {
            stringValue.value = value.toString();
            return true;
        });
    }

    public static class Product {
        private String name;
        private Money price;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Money getPrice() {
            return price;
        }

        public void setPrice(Money price) {
            this.price = price;
        }
    }

    public static class MoneySerializer extends StdSerializer<Money> {

        public MoneySerializer() {
            super(Money.class);
        }

        @Override
        public void serialize(Money value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }

    public static class MoneyDeserializer extends StdDeserializer<Money> {
        public MoneyDeserializer() {
            super(Money.class);
        }

        @Override
        public Money deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String s = p.getText();
            String[] split = s.split(" ");
            return new Money(Integer.valueOf(split[0]), split[1]);
        }
    }

    public static class Money {
        private String currency;
        private int amount;

        public Money(int amount, String currency) {
            this.currency = currency;
            this.amount = amount;
        }

        public Money() {
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public static Money forDollars(int amount) {
            return new Money(amount, "USD");
        }

        public static Money forEuro(int amount) {
            return new Money(amount, "EUR");
        }

        @Override
        public String toString() {
            return amount + " " + currency;
        }
    }
}
