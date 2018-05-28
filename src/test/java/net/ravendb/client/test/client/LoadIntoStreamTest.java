package net.ravendb.client.test.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.extensions.JsonExtensions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadIntoStreamTest extends RemoteTestBase {

    @Test
    public void canLoadByIdsIntoStream() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            insertData(store);

            try (IDocumentSession session = store.openSession()) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                List<String> ids = Arrays.asList("employees/1-A", "employees/4-A", "employees/7-A");
                session.advanced().loadIntoStream(ids, stream);

                JsonNode jsonNode = JsonExtensions.getDefaultMapper().readTree(stream.toByteArray());

                ArrayNode res = (ArrayNode) jsonNode.get("Results");

                assertThat(res.size())
                        .isEqualTo(3);


                List<String> names = new ArrayList<>(Arrays.asList("Aviv", "Maxim", "Michael"));
                for (JsonNode v : res) {
                    String name = v.get("firstName").asText();

                    assertThat(name)
                            .isIn(names);
                    names.remove(name);
                }
            }
        }
    }

    @Test
    public void canLoadStartingWithIntoStream() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            insertData(store);

            try (IDocumentSession session = store.openSession()) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                session.advanced().loadStartingWithIntoStream("employees/", stream);

                JsonNode jsonNode = JsonExtensions.getDefaultMapper().readTree(stream.toByteArray());

                ArrayNode res = (ArrayNode) jsonNode.get("Results");

                assertThat(res.size())
                        .isEqualTo(7);

                List<String> names = new ArrayList<>(Arrays.asList("Aviv", "Iftah", "Tal", "Maxim", "Karmel", "Grisha", "Michael" ));
                for (JsonNode v : res) {
                    String name = v.get("firstName").asText();

                    assertThat(name)
                            .isIn(names);
                    names.remove(name);
                }
            }
        }
    }

    private static void insertData(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            Consumer<String> insertEmployee = name -> {
                Employee employee = new Employee();
                employee.setFirstName(name);
                session.store(employee);
            };

            insertEmployee.accept("Aviv");
            insertEmployee.accept("Iftah");
            insertEmployee.accept("Tal");
            insertEmployee.accept("Maxim");
            insertEmployee.accept("Karmel");
            insertEmployee.accept("Grisha");
            insertEmployee.accept("Michael");
            session.saveChanges();
        }
    }

    public static class Employee {
        private String firstName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
    }
}
