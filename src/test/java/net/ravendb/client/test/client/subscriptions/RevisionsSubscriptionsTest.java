package net.ravendb.client.test.client.subscriptions;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsOperation;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.subscriptions.Revision;
import net.ravendb.client.documents.subscriptions.SubscriptionBatch;
import net.ravendb.client.documents.subscriptions.SubscriptionWorker;
import net.ravendb.client.documents.subscriptions.SubscriptionWorkerOptions;
import net.ravendb.client.infrastructure.DisabledOn41Server;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class RevisionsSubscriptionsTest extends RemoteTestBase {

    private final int _reasonableWaitTime = 15;

    @Test
    @DisabledOn41Server
    public void plainRevisionsSubscriptions() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String subscriptionId = store.subscriptions().createForRevisions(User.class);

            RevisionsCollectionConfiguration defaultCollection = new RevisionsCollectionConfiguration();
            defaultCollection.setDisabled(false);
            defaultCollection.setMinimumRevisionsToKeep(5L);

            RevisionsCollectionConfiguration usersConfig = new RevisionsCollectionConfiguration();
            usersConfig.setDisabled(false);

            RevisionsCollectionConfiguration donsConfig = new RevisionsCollectionConfiguration();
            donsConfig.setDisabled(false);

            RevisionsConfiguration configuration = new RevisionsConfiguration();
            configuration.setDefaultConfig(defaultCollection);

            HashMap<String, RevisionsCollectionConfiguration> perCollectionConfig = new HashMap<>();
            perCollectionConfig.put("Users", usersConfig);
            perCollectionConfig.put("Dons", donsConfig);

            configuration.setCollections(perCollectionConfig);

            ConfigureRevisionsOperation operation = new ConfigureRevisionsOperation(configuration);

            store.maintenance().send(operation);

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    try (IDocumentSession session = store.openSession()) {
                        User user = new User();
                        user.setName("users" + i + " ver " + j);
                        session.store(user, "users/" + i);

                        Company company = new Company();
                        company.setName("dons" + i + " ver " + j);
                        session.store(company, "dons/" + i);

                        session.saveChanges();
                    }
                }
            }

            try (SubscriptionWorker<Revision<User>> sub = store.subscriptions().getSubscriptionWorkerForRevisions(User.class, new SubscriptionWorkerOptions(subscriptionId))) {
                Semaphore mre = new Semaphore(0);
                Set<String> names = new HashSet<>();

                sub.run(x -> {
                    for (SubscriptionBatch.Item<Revision<User>> item : x.getItems()) {
                        Revision<User> result = item.getResult();

                        names.add((result.getCurrent() != null ? result.getCurrent().getName() : null) + (result.getPrevious() != null ? result.getPrevious().getName() : null));

                        if (names.size() == 100) {
                            mre.release();
                        }
                    }
                });

                assertThat(mre.tryAcquire(_reasonableWaitTime, TimeUnit.SECONDS))
                        .isTrue();

            }
        }
    }

    @Test
    @DisabledOn41Server
    public void plainRevisionsSubscriptionsCompareDocs() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String subscriptionId = store.subscriptions().createForRevisions(User.class);

            RevisionsCollectionConfiguration defaultCollection = new RevisionsCollectionConfiguration();
            defaultCollection.setDisabled(false);
            defaultCollection.setMinimumRevisionsToKeep(5L);

            RevisionsCollectionConfiguration usersConfig = new RevisionsCollectionConfiguration();
            usersConfig.setDisabled(false);

            RevisionsCollectionConfiguration donsConfig = new RevisionsCollectionConfiguration();
            donsConfig.setDisabled(false);

            RevisionsConfiguration configuration = new RevisionsConfiguration();
            configuration.setDefaultConfig(defaultCollection);

            HashMap<String, RevisionsCollectionConfiguration> perCollectionConfig = new HashMap<>();
            perCollectionConfig.put("Users", usersConfig);
            perCollectionConfig.put("Dons", donsConfig);

            configuration.setCollections(perCollectionConfig);

            ConfigureRevisionsOperation operation = new ConfigureRevisionsOperation(configuration);

            store.maintenance().send(operation);


            for (int j = 0; j < 10; j++) {
                try (IDocumentSession session = store.openSession()) {
                    User user = new User();
                    user.setName("users1 ver " + j);
                    user.setAge(j);
                    session.store(user, "users/1");

                    Company company = new Company();
                    company.setName("dons1 ver " + j);
                    session.store(company, "dons/1");

                    session.saveChanges();
                }
            }

            try (SubscriptionWorker<Revision<User>> sub = store.subscriptions().getSubscriptionWorkerForRevisions(User.class, new SubscriptionWorkerOptions(subscriptionId))) {
                Semaphore mre = new Semaphore(0);
                Set<String> names = new HashSet<>();

                final AtomicInteger maxAge = new AtomicInteger(-1);

                sub.run(a -> {
                    for (SubscriptionBatch.Item<Revision<User>> item : a.getItems()) {
                        Revision<User> x = item.getResult();
                        if (x.getCurrent().getAge() > maxAge.get() && x.getCurrent().getAge() > Optional.ofNullable(x.getPrevious()).map(y -> y.getAge()).orElse(-1)) {
                            names.add(Optional.ofNullable(x.getCurrent()).map(y -> y.getName()).orElse(null) + " "
                                    +  Optional.ofNullable(x.getPrevious()).map(y -> y.getName()).orElse(null));
                            maxAge.set(x.getCurrent().getAge());
                        }

                        if (names.size() == 10) {
                            mre.release();
                        }
                    }
                });

                assertThat(mre.tryAcquire(_reasonableWaitTime, TimeUnit.SECONDS))
                        .isTrue();

            }
        }
    }
}
