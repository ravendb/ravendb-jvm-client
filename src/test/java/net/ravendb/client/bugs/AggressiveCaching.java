package net.ravendb.client.bugs;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.CleanCloseable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class AggressiveCaching extends RemoteTestBase {

    public DocumentStore initAggressiveCaching() throws Exception {
        DocumentStore store = getDocumentStore();
        store.disableAggressiveCaching();

        try (IDocumentSession session = store.openSession()) {
            session.store(new User());
            session.saveChanges();
        }

        return store;
    }

    @Test
    public void canAggressivelyCacheLoads_404() throws Exception {
        try (IDocumentStore store = initAggressiveCaching()) {
            RequestExecutor requestExecutor = store.getRequestExecutor();

            long oldNumOfRequests = requestExecutor.numberOfServerRequests.get();
            for (int i = 0; i < 5; i++) {
                try (IDocumentSession session = store.openSession()) {
                    try (CleanCloseable context = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMinutes(5))) {
                        session.load(User.class, "users/not-there");
                    }
                }
            }

            assertThat(requestExecutor.numberOfServerRequests.get())
                    .isEqualTo(1 + oldNumOfRequests);
        }
    }

    @Test
    public void canAggressivelyCacheLoads() throws Exception {
        try (IDocumentStore store = initAggressiveCaching()) {
            RequestExecutor requestExecutor = store.getRequestExecutor();

            long oldNumOfRequests = requestExecutor.numberOfServerRequests.get();
            for (int i = 0; i < 5; i++) {
                try (IDocumentSession session = store.openSession()) {
                    try (CleanCloseable context = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMinutes(5))) {
                        session.load(User.class, "users/1-A");
                    }
                }
            }

            assertThat(requestExecutor.numberOfServerRequests.get())
                    .isEqualTo(1 + oldNumOfRequests);
        }
    }

    @Test
    public void canAggressivelyCacheQueries() throws Exception {
        try (IDocumentStore store = initAggressiveCaching()) {
            RequestExecutor requestExecutor = store.getRequestExecutor();

            long oldNumOfRequests = requestExecutor.numberOfServerRequests.get();
            for (int i = 0; i < 5; i++) {
                try (IDocumentSession session = store.openSession()) {
                    try (CleanCloseable context = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMinutes(5))) {
                        session.query(User.class).toList();
                    }
                }
            }

            assertThat(requestExecutor.numberOfServerRequests.get())
                    .isEqualTo(1 + oldNumOfRequests);
        }
    }

    @Test
    public void waitForNonStaleResultsIgnoresAggressiveCaching() throws Exception {
        try (IDocumentStore store = initAggressiveCaching()) {
            RequestExecutor requestExecutor = store.getRequestExecutor();

            long oldNumOfRequests = requestExecutor.numberOfServerRequests.get();
            for (int i = 0; i < 5; i++) {
                try (IDocumentSession session = store.openSession()) {
                    try (CleanCloseable context = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMinutes(5))) {
                        session
                                .query(User.class)
                                .waitForNonStaleResults()
                                .toList();
                    }
                }
            }

            assertThat(requestExecutor.numberOfServerRequests.get())
                    .isNotEqualTo(1 + oldNumOfRequests);
        }
    }
}
