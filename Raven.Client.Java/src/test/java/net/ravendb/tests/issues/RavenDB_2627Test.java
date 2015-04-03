package net.ravendb.tests.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.basic.VoidArgs;
import net.ravendb.abstractions.closure.Function0;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.SubscriptionBatchOptions;
import net.ravendb.abstractions.data.SubscriptionConfig;
import net.ravendb.abstractions.data.SubscriptionConnectionOptions;
import net.ravendb.abstractions.data.SubscriptionCriteria;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionDoesNotExistExeption;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionInUseException;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.json.linq.RavenJValue;
import net.ravendb.abstractions.util.TimeUtils;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.RemoteClientTest;
import net.ravendb.client.changes.ObserverAdapter;
import net.ravendb.client.document.BulkInsertOperation;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.client.document.Subscription;
import net.ravendb.tests.bugs.User;
import net.ravendb.tests.common.dto.Address;
import net.ravendb.tests.common.dto.PersonWithAddress;
import net.ravendb.tests.document.Company;
import net.ravendb.tests.json.WebinarTest.Person;
import net.ravendb.utils.SpinWait;

import org.junit.Test;


public class RavenDB_2627Test extends RemoteClientTest {

  @Test
  public void canCreateSubscription() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      long id = store.subscriptions().create(new SubscriptionCriteria());
      assertEquals(1, id);

      id = store.subscriptions().create(new SubscriptionCriteria());
      assertEquals(2, id);
    }
  }

  @Test
  public void canDeleteSubscription() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      long id1 = store.subscriptions().create(new SubscriptionCriteria());
      long id2 = store.subscriptions().create(new SubscriptionCriteria());

      List<SubscriptionConfig> subscriptions = store.subscriptions().getSubscriptions(0, 5);

      assertEquals(2, subscriptions.size());

      store.subscriptions().delete(id1);
      store.subscriptions().delete(id2);

      subscriptions = store.subscriptions().getSubscriptions(0, 5);

      assertEquals(0, subscriptions.size());
    }
  }

  @Test
  public void shouldThrowWhenOpeningNoExisingSubscription() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      try {
        store.subscriptions().open(1, new SubscriptionConnectionOptions());
        fail();
      } catch (SubscriptionDoesNotExistExeption e) {
        assertEquals("There is no subscription configuration for specified identifier (id: 1)", e.getMessage());
      }
    }
  }

  @SuppressWarnings("unused")
  @Test
  public void shouldThrowOnAttemptToOpenAlreadyOpenedSubscription() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      long id = store.subscriptions().create(new SubscriptionCriteria());
      store.subscriptions().open(1, new SubscriptionConnectionOptions());

      try {
        store.subscriptions().open(1, new SubscriptionConnectionOptions());
        fail();
      } catch (SubscriptionInUseException e) {
        assertEquals("Subscription is already in use. There can be only a single open subscription connection per subscription.", e.getMessage());
      }
    }
  }

  private final static int WAIT_TIME_OUT_SECONDS = 20;

  @SuppressWarnings("boxing")
  @Test
  public void shouldStreamAllDocumentsAfterSubscriptionCreation() throws InterruptedException {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {

      storeUserAged(store, 31, "users/1");
      storeUserAged(store, 27, "users/12");
      storeUserAged(store, 25, "users/3");

      long id = store.subscriptions().create(new SubscriptionCriteria());
      Subscription<RavenJObject> subscription = store.subscriptions().open(id, new SubscriptionConnectionOptions());

      final BlockingQueue<String> keys = new ArrayBlockingQueue<>(100);
      final BlockingQueue<Integer> ages = new ArrayBlockingQueue<>(100);

      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          keys.offer(value.get(Constants.METADATA).value(String.class, "@id"));
        }
      });

      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          ages.offer(value.value(Integer.class, "Age"));
        }
      });

      String key = keys.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertEquals("users/1", key);

      key = keys.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertEquals("users/12", key);

      key = keys.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertEquals("users/3", key);

      int age = ages.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertEquals(31, age);

      age = ages.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertEquals(27, age);

      age = ages.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertEquals(25, age);
    }
  }

  private static void storeUserAged(IDocumentStore store, int age, String id) {
    try (IDocumentSession session = store.openSession()) {
      User u = new User();
      u.setAge(age);
      session.store(u, id);
      session.saveChanges();
    }
  }

  @Test
  public void shouldSendAllNewAndModifiedDocs() throws InterruptedException {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {

      long id = store.subscriptions().create(new SubscriptionCriteria());
      Subscription<RavenJObject> subscription = store.subscriptions().open(id, new SubscriptionConnectionOptions());

      final BlockingQueue<String> names = new ArrayBlockingQueue<>(100);
      store.changes().waitForAllPendingSubscriptions();

      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          names.add(value.value(String.class, "Name"));
        }
      });

      try (IDocumentSession session = store.openSession()) {
        User user = new User();
        user.setName("James");
        session.store(user, "users/1");
        session.saveChanges();
      }

      String name = names.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(name);
      assertEquals("James", name);

      try (IDocumentSession session = store.openSession()) {
        User user = new User();
        user.setName("Adam");
        session.store(user, "users/12");
        session.saveChanges();
      }

      name = names.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(name);
      assertEquals("Adam", name);

      try (IDocumentSession session = store.openSession()) {
        User user = new User();
        user.setName("David");
        session.store(user, "users/1");
        session.saveChanges();
      }

      name = names.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(name);
      assertEquals("David", name);
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void shouldResendDocsIfAcknowledgmentTimeoutOccurred() throws InterruptedException {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      long id = store.subscriptions().create(new SubscriptionCriteria());

      SubscriptionBatchOptions batchOptions = new SubscriptionBatchOptions();
      batchOptions.setAcknowledgmentTimeout(-10L); // the client won't be able to acknowledge in negative time
      SubscriptionConnectionOptions connectionOptions = new SubscriptionConnectionOptions();
      connectionOptions.setBatchOptions(batchOptions);
      Subscription<RavenJObject> subscriptionZeroTimeout = store.subscriptions().open(id, connectionOptions);

      store.changes().waitForAllPendingSubscriptions();

      final BlockingQueue<RavenJObject> docs = new ArrayBlockingQueue<>(100);
      subscriptionZeroTimeout.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      try (IDocumentSession session = store.openSession()) {
        User user = new User();
        user.setName("Raven");
        session.store(user);
        session.saveChanges();
      }

      RavenJObject document;

      document = docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(document);
      assertEquals("Raven", document.value(String.class, "Name"));

      document = docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(document);
      assertEquals("Raven", document.value(String.class, "Name"));

      document = docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(document);
      assertEquals("Raven", document.value(String.class, "Name"));

      subscriptionZeroTimeout.close();

      // retry with longer timeout - should sent just one doc

      Subscription<RavenJObject> subscriptionLongerTimeout = store.subscriptions().open(id,
        new SubscriptionConnectionOptions(new SubscriptionBatchOptions(null, 4096, 30 * 1000L)));

      final BlockingQueue<RavenJObject> docs2 = new ArrayBlockingQueue<>(100);

      subscriptionLongerTimeout.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs2.add(value);
        }
      });

      document = docs2.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(document);
      assertEquals("Raven", document.value(String.class, "Name"));

      document = docs2.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNull(document);
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void shouldRespectMaxDocCountInBatch() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      try (IDocumentSession session = store.openSession()) {
        for (int i = 0; i< 100; i++) {
          session.store(new Company());
        }
        session.saveChanges();
      }

      long id = store.subscriptions().create(new SubscriptionCriteria());
      SubscriptionBatchOptions batchOptions = new SubscriptionBatchOptions();
      batchOptions.setMaxDocCount(25);
      Subscription<RavenJObject> subscription = store.subscriptions().open(id, new SubscriptionConnectionOptions(batchOptions));

      final List<Reference<Integer>> batchSizes = new ArrayList<>();
      subscription.addBeforeBatchHandler(new EventHandler<VoidArgs>() {
        @Override
        public void handle(Object sender, VoidArgs event) {
          batchSizes.add(new Reference<>(0));
        }
      });
      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          Reference<Integer> reference = batchSizes.get(batchSizes.size() - 1);
          reference.value += 1;
        }
      });

      boolean result = SpinWait.spinUntil(new Function0<Boolean>() {
        @Override
        public Boolean apply() {
          int total = 0;
          for (Reference<Integer> ref: batchSizes) {
            total += ref.value;
          }
          return total >= 100;
        }
      }, 60 * 1000L);

      assertTrue(result);

      assertEquals(4, batchSizes.size());

      for (Reference<Integer> reference : batchSizes) {
        assertEquals(Integer.valueOf(25), reference.value);
      }
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void shouldRespectMaxBatchSize() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      try (IDocumentSession session = store.openSession()) {
        for (int i = 0; i< 100; i++) {
          session.store(new Company());
          session.store(new User());
        }
        session.saveChanges();
      }

      long id = store.subscriptions().create(new SubscriptionCriteria());
      SubscriptionBatchOptions batchOptions = new SubscriptionBatchOptions();
      batchOptions.setMaxSize(16 * 1024);
      Subscription<RavenJObject> subscription = store.subscriptions().open(id, new SubscriptionConnectionOptions(batchOptions));

      final List<List<RavenJObject>> batches = new ArrayList<>();
      subscription.addBeforeBatchHandler(new EventHandler<VoidArgs>() {
        @Override
        public void handle(Object sender, VoidArgs event) {
          batches.add(new ArrayList<RavenJObject>());
        }
      });
      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          List<RavenJObject> list = batches.get(batches.size() - 1);
          list.add(value);
        }
      });

      boolean result = SpinWait.spinUntil(new Function0<Boolean>() {
        @Override
        public Boolean apply() {
          int total = 0;
          for (List<RavenJObject> list: batches) {
            total += list.size();
          }
          return total >= 200;
        }
      }, 60 * 1000L);

      assertTrue(result);
      assertTrue(batches.size() > 1);
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void shouldRespectCollectionCriteria() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      try (IDocumentSession session = store.openSession()) {
        for (int i = 0; i< 100; i++) {
          session.store(new Company());
          session.store(new User());
        }
        session.saveChanges();
      }

      SubscriptionCriteria criteria = new SubscriptionCriteria();
      criteria.setBelongsToAnyCollection("Users");
      long id = store.subscriptions().create(criteria);

      SubscriptionBatchOptions batchOptions = new SubscriptionBatchOptions();
      batchOptions.setMaxDocCount(31);
      Subscription<RavenJObject> subscription = store.subscriptions().open(id, new SubscriptionConnectionOptions(batchOptions));

      final List<RavenJObject> docs = new ArrayList<>();
      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      assertTrue(SpinWait.spinUntil(new Function0<Boolean>() {
        @Override
        public Boolean apply() {
          return docs.size() >= 100;
        }
      }, 60 * 1000L));

      for (RavenJObject jsonDocument : docs) {
        assertEquals("Users", jsonDocument.get(Constants.METADATA).value(String.class, Constants.RAVEN_ENTITY_NAME));
      }
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void shouldRespectStartsWithCriteria() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      try (IDocumentSession session = store.openSession()) {
        for (int i = 0; i< 100; i++) {
          session.store(new User(), i % 2 == 0 ? "users/" : "users/favorite/");
        }
        session.saveChanges();
      }

      SubscriptionCriteria criteria = new SubscriptionCriteria();
      criteria.setKeyStartsWith("users/favorite/");
      long id = store.subscriptions().create(criteria);

      SubscriptionBatchOptions batchOptions = new SubscriptionBatchOptions();
      batchOptions.setMaxDocCount(15);

      Subscription<RavenJObject> subscription = store.subscriptions().open(id, new SubscriptionConnectionOptions(batchOptions));

      final List<RavenJObject> docs = new ArrayList<>();
      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      assertTrue(SpinWait.spinUntil(new Function0<Boolean>() {
        @Override
        public Boolean apply() {
          return docs.size() >= 50;
        }
      }, 60 * 1000L));

      for (RavenJObject jsonDocument : docs) {
        assertTrue(jsonDocument.get(Constants.METADATA).value(String.class, "@id").startsWith("users/favorite/"));
      }
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void shouldRespectPropertiesCriteria() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      try (IDocumentSession session = store.openSession()) {
        for (int i = 0; i < 10; i++) {
          String name = i % 2 == 0 ? "Jessica" : "Caroline";
          User u = new User();
          u.setName(name);
          session.store(u);

          name = i % 2 == 0 ? "Caroline" : "Samantha";
          Person p = new Person();
          p.setName(name);
          session.store(p);

          session.store(new Company());
        }
        session.saveChanges();
      }

      SubscriptionCriteria criteria = new SubscriptionCriteria();
      Map<String, RavenJToken> properties = new HashMap<>();
      properties.put("Name", RavenJToken.fromObject("Caroline"));
      criteria.setPropertiesMatch(properties);

      long id = store.subscriptions().create(criteria);

      SubscriptionBatchOptions batchOptions = new SubscriptionBatchOptions();
      batchOptions.setMaxDocCount(5);
      SubscriptionConnectionOptions options = new SubscriptionConnectionOptions(batchOptions);

      Subscription<RavenJObject> carolines = store.subscriptions().open(id, options);

      final List<RavenJObject> docs = new ArrayList<>();
      carolines.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      assertTrue(SpinWait.spinUntil(new Function0<Boolean>() {
        @Override
        public Boolean apply() {
          return docs.size() >= 10;
        }
      }, 60 * 1000L));

      for (RavenJObject jsonDocument : docs) {
        assertEquals("Caroline", jsonDocument.value(String.class, "Name"));
      }
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void shouldRespectPropertiesNotMatchCriteria() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      try (IDocumentSession session = store.openSession()) {
        for (int i = 0; i < 10; i++) {
          String name = i % 2 == 0 ? "Jessica" : "Caroline";
          User u = new User();
          u.setName(name);
          session.store(u);

          name = i % 2 == 0 ? "Caroline" : "Samantha";
          Person p = new Person();
          p.setName(name);
          session.store(p);

          session.store(new Company());
        }
        session.saveChanges();
      }

      SubscriptionCriteria criteria = new SubscriptionCriteria();
      Map<String, RavenJToken> properties = new HashMap<>();
      properties.put("Name", RavenJToken.fromObject("Caroline"));
      criteria.setPropertiesNotMatch(properties);

      long id = store.subscriptions().create(criteria);

      SubscriptionBatchOptions batchOptions = new SubscriptionBatchOptions();
      batchOptions.setMaxDocCount(5);
      SubscriptionConnectionOptions options = new SubscriptionConnectionOptions(batchOptions);

      Subscription<RavenJObject> carolines = store.subscriptions().open(id, options);

      final List<RavenJObject> docs = new ArrayList<>();
      carolines.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      assertTrue(SpinWait.spinUntil(new Function0<Boolean>() {
        @Override
        public Boolean apply() {
          return docs.size() >= 20;
        }
      }, 60 * 1000L));

      for (RavenJObject jsonDocument : docs) {
        assertTrue(jsonDocument.containsKey("Name") == false || !"Caroline".equals(jsonDocument.value(String.class, "Name")));
      }
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void canGetSubscriptionsFromDatabase() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      List<SubscriptionConfig> subscriptionDocuments = store.subscriptions().getSubscriptions(0, 10);

      assertEquals(0, subscriptionDocuments.size());

      SubscriptionCriteria criteria = new SubscriptionCriteria();
      criteria.setKeyStartsWith("users/");
      store.subscriptions().create(criteria);

      subscriptionDocuments = store.subscriptions().getSubscriptions(0, 10);
      assertEquals(1, subscriptionDocuments.size());
      assertEquals("users/", subscriptionDocuments.get(0).getCriteria().getKeyStartsWith());

      Subscription<RavenJObject> subscription = store.subscriptions().open(subscriptionDocuments.get(0).getSubscriptionId(), new SubscriptionConnectionOptions());

      final List<RavenJObject> docs = new ArrayList<>();
      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      try (IDocumentSession session = store.openSession()) {
        session.store(new User());
        session.saveChanges();
      }

      assertTrue(SpinWait.spinUntil(new Function0<Boolean>() {
        @Override
        public Boolean apply() {
          return docs.size() >= 1;
        }
      }, 60 * 1000L));
    }
  }

  protected boolean serverDisposed = false;

  @SuppressWarnings("boxing")
  @Test
  public void shouldKeepPullingDocsAfterServerRestart() throws InterruptedException {
    IDocumentStore store = null;
    try {
      serverDisposed = false;
      RavenJObject serverDocument = getCreateServerDocument(DEFAULT_SERVER_PORT_2);
      serverDocument.add("RunInMemory", new RavenJValue(false));

      startServer(DEFAULT_SERVER_PORT_2, true, serverDocument);

      store = new DocumentStore("http://localhost:" + DEFAULT_SERVER_PORT_2, "RavenDB_2627");
      store.initialize();

      store.getDatabaseCommands().getGlobalAdmin().ensureDatabaseExists("RavenDB_2627");

      try (IDocumentSession session = store.openSession()) {
        session.store(new User());
        session.store(new User());
        session.store(new User());
        session.store(new User());

        session.saveChanges();
      }

      long id = store.subscriptions().create(new SubscriptionCriteria());

      SubscriptionBatchOptions batchOptions = new SubscriptionBatchOptions();
      batchOptions.setMaxDocCount(1);
      SubscriptionConnectionOptions connectionOptions = new SubscriptionConnectionOptions(batchOptions);
      Subscription<RavenJObject> subscription = store.subscriptions().open(id, connectionOptions);

      store.changes().waitForAllPendingSubscriptions();

      CleanCloseable serverDisposingHandler = subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @SuppressWarnings("synthetic-access")
        @Override
        public void onNext(RavenJObject value) {
          stopServer(DEFAULT_SERVER_PORT_2);
          TimeUtils.cleanSleep(1000);
          serverDisposed = true;
        }
      });

      SpinWait.spinUntil(new Function0<Boolean>() {
        @Override
        public Boolean apply() {
          return serverDisposed;
        }
      }, 30 * 1000L);

      serverDisposingHandler.close();

      final BlockingQueue<RavenJObject> docs = new ArrayBlockingQueue<>(100);
      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      startServer(DEFAULT_SERVER_PORT_2, false, serverDocument);

      assertNotNull(docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS));
      assertNotNull(docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS));
      assertNotNull(docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS));
      assertNotNull(docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS));

      try (IDocumentSession session = store.openSession()) {
        session.store(new User(), "users/arek");
        session.saveChanges();
      }

      RavenJObject arekObject = docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(arekObject);
      assertEquals("users/arek", arekObject.get(Constants.METADATA).value(String.class, "@id"));

    } finally {
      if (store != null) {
        store.close();
      }
      stopServer(DEFAULT_SERVER_PORT_2);
    }
  }

  @Test
  public void shouldStopPullingDocsIfThereIsNoSubscriber() throws InterruptedException {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      long id = store.subscriptions().create(new SubscriptionCriteria());

      Subscription<RavenJObject> subscription = store.subscriptions().open(id, new SubscriptionConnectionOptions());
      store.changes().waitForAllPendingSubscriptions();

      try (IDocumentSession session = store.openSession()) {
        session.store(new User(), "users/1");
        session.store(new User(), "users/2");
        session.saveChanges();
      }

      final BlockingQueue<RavenJObject> docs = new ArrayBlockingQueue<>(100);
      CleanCloseable subscribe = subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      RavenJObject doc;
      doc = docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(doc);

      doc = docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(doc);

      subscribe.close();

      try (IDocumentSession session = store.openSession()) {
        session.store(new User(), "users/3");
        session.saveChanges();
      }

      try (IDocumentSession session = store.openSession()) {
        session.store(new User(), "users/4");
        session.saveChanges();
      }

      TimeUtils.cleanSleep(5 * 1000);  // should not pull any docs because there is no subscriber that could process them

      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      doc = docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(doc);
      assertEquals("users/3", doc.get(Constants.METADATA).value(String.class, "@id"));

      doc = docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(doc);
      assertEquals("users/4", doc.get(Constants.METADATA).value(String.class, "@id"));
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void shouldAllowToOpenSubscriptionIfClientDidntSentAliveNotificationOnTime() throws InterruptedException {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      try (IDocumentSession session = store.openSession()) {
        session.store(new User());
        session.saveChanges();
      }

      long id = store.subscriptions().create(new SubscriptionCriteria());
      SubscriptionConnectionOptions connectionOptions = new SubscriptionConnectionOptions();
      connectionOptions.setClientAliveNotificationInterval(2 * 1000);
      Subscription<RavenJObject> subscription = store.subscriptions().open(id, connectionOptions);
      store.changes().waitForAllPendingSubscriptions();

      subscription.addAfterBatchHandler(new EventHandler<VoidArgs>() {
        @Override
        public void handle(Object sender, VoidArgs event) {
          TimeUtils.cleanSleep(20 * 1000); // to prevent the client from sending client-alive notification
        }
      });

      final BlockingQueue<RavenJObject> docs = new ArrayBlockingQueue<>(100);
      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      store.changes().waitForAllPendingSubscriptions();

      RavenJObject _ = docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(_);

      TimeUtils.cleanSleep(10 * 1000);

      // first open subscription didn't send the client-alive notification in time, so the server should allow to open it for this subscription
      Subscription<RavenJObject> newSubscription = store.subscriptions().open(id, new SubscriptionConnectionOptions());

      final BlockingQueue<RavenJObject> docs2 = new ArrayBlockingQueue<>(100);
      newSubscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs2.add(value);
        }
      });

      try (IDocumentSession session = store.openSession()) {
        session.store(new User());
        session.saveChanges();
      }

      assertNotNull(docs2.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS));
      assertNull(docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS));
    }
  }

  @Test
  public void canReleaseSubscription() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      long id = store.subscriptions().create(new SubscriptionCriteria());
      store.subscriptions().open(id, new SubscriptionConnectionOptions());
      store.changes().waitForAllPendingSubscriptions();
      try {
        store.subscriptions().open(id, new SubscriptionConnectionOptions());
        fail();
      } catch(SubscriptionInUseException e) {
        //ok
      }

      store.subscriptions().release(id);

      store.subscriptions().open(id, new SubscriptionConnectionOptions());
    }
  }

  @Test
  public void shouldPullDocumentsAfterBulkInsert() throws InterruptedException {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      long id = store.subscriptions().create(new SubscriptionCriteria());
      Subscription<RavenJObject> subscription = store.subscriptions().open(id, new SubscriptionConnectionOptions());
      store.changes().waitForAllPendingSubscriptions();

      final BlockingQueue<RavenJObject> docs = new ArrayBlockingQueue<>(100);
      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      store.changes().waitForAllPendingSubscriptions();

      try (BulkInsertOperation bulk = store.bulkInsert()) {
        bulk.store(new User());
        bulk.store(new User());
        bulk.store(new User());
      }

      RavenJObject doc;
      doc = docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(doc);

      doc = docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(doc);
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void shouldStopPullingDocsAndCloseSubscriptionOnSubscriberErrorByDefault() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      long id = store.subscriptions().create(new SubscriptionCriteria());
      final Subscription<RavenJObject> subscription = store.subscriptions().open(id, new SubscriptionConnectionOptions());

      final BlockingQueue<RavenJObject> docs = new ArrayBlockingQueue<>(100);
      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      final Reference<Boolean> subscriberException = new Reference<>(false);

      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          throw new RuntimeException("Fake exception");
        }
        @Override
        public void onError(Exception error) {
          subscriberException.value = true;
        }
      });

      store.changes().waitForAllPendingSubscriptions();

      store.getDatabaseCommands().put("items/1", null, new RavenJObject(), new RavenJObject());

      assertTrue(SpinWait.spinUntil(new Function0<Boolean>() {
        @Override
        public Boolean apply() {
          return subscriberException.value;
        }
      }, WAIT_TIME_OUT_SECONDS * 1000L));

      assertTrue(subscription.isErrored());

      assertTrue(SpinWait.spinUntil(new Function0<Boolean>() {
        @Override
        public Boolean apply() {
          return subscription.isClosed();
        }
      }, WAIT_TIME_OUT_SECONDS * 1000L));

      SubscriptionConfig subscriptionConfig = store.subscriptions().getSubscriptions(0, 1).get(0);
      assertEquals(Etag.empty(), subscriptionConfig.getAckEtag());
    }
  }

  @Test
  public void canSetToIgnoreSubscriberErrors() throws InterruptedException {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      long id = store.subscriptions().create(new SubscriptionCriteria());
      SubscriptionConnectionOptions connectionOptions = new SubscriptionConnectionOptions();
      connectionOptions.setIgnoreSubscribersErrors(true);
      Subscription<RavenJObject> subscription = store.subscriptions().open(id, connectionOptions);
      store.changes().waitForAllPendingSubscriptions();

      final BlockingQueue<RavenJObject> docs = new ArrayBlockingQueue<>(100);
      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          throw new RuntimeException("Fake exception");
        }
      });

      store.changes().waitForAllPendingSubscriptions();

      store.getDatabaseCommands().put("items/1", null, new RavenJObject(), new RavenJObject());
      store.getDatabaseCommands().put("items/2", null, new RavenJObject(), new RavenJObject());

      RavenJObject doc;
      doc = docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(doc);

      doc = docs.poll(WAIT_TIME_OUT_SECONDS, TimeUnit.SECONDS);
      assertNotNull(doc);

      assertFalse(subscription.isErrored());
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void canUseNestedPropertiesInSubscriptionCriteria() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      try (IDocumentSession session = store.openSession()) {
        for (int i =0 ; i < 10; i++) {
          PersonWithAddress personWithAddress1 = new PersonWithAddress();
          Address address1 = new Address();
          personWithAddress1.setAddress(address1);
          address1.setStreet("1st Street");
          address1.setZipCode(i % 2 == 0 ? 999 : 12345);
          session.store(personWithAddress1);

          PersonWithAddress personWithAddress2 = new PersonWithAddress();
          Address address2 = new Address();
          personWithAddress2.setAddress(address2);
          address2.setStreet("2nd Street");
          address2.setZipCode(12345);
          session.store(personWithAddress2);

          session.store(new Company());
        }
        session.saveChanges();
      }

      SubscriptionCriteria criteria = new SubscriptionCriteria();
      Map<String, RavenJToken> match = new HashMap<>();
      match.put("Address.Street", RavenJToken.fromObject("1st Street"));
      Map<String, RavenJToken> notMatch = new HashMap<>();
      notMatch.put("Address.ZipCode", RavenJToken.fromObject(999));
      criteria.setPropertiesMatch(match);
      criteria.setPropertiesNotMatch(notMatch);
      long id = store.subscriptions().create(criteria);
      SubscriptionBatchOptions batchOptions = new SubscriptionBatchOptions();
      batchOptions.setMaxDocCount(5);
      Subscription<RavenJObject> subscription = store.subscriptions().open(id, new SubscriptionConnectionOptions(batchOptions));

      final BlockingQueue<RavenJObject> docs = new ArrayBlockingQueue<>(100);
      subscription.subscribe(new ObserverAdapter<RavenJObject>() {
        @Override
        public void onNext(RavenJObject value) {
          docs.add(value);
        }
      });

      assertTrue(SpinWait.spinUntil(new Function0<Boolean>() {
        @Override
        public Boolean apply() {
          return docs.size() >= 5;
        }
      }, 60 * 1000L));

      for (RavenJObject jsonDocument: docs) {
        assertEquals("1st Street", jsonDocument.value(RavenJObject.class, "Address").value(String.class, "Street"));
      }
    }
  }
}
