package net.ravendb.tests.shard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.ravendb.abstractions.closure.Action1;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.ITransactionalDocumentSession;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.document.DocumentKeyGenerator;
import net.ravendb.client.shard.IShardResolutionStrategy;
import net.ravendb.client.shard.SequentialShardAccessStrategy;
import net.ravendb.client.shard.ShardRequestData;
import net.ravendb.client.shard.ShardStrategy;
import net.ravendb.client.shard.ShardedDocumentStore;
import net.ravendb.tests.bundles.replication.ReplicationBase;
import net.ravendb.tests.document.Company;

import org.junit.Test;
import org.mockito.Mockito;


public class WhenUsingShardedServersTest extends ReplicationBase {

  protected Map<String, IDocumentStore> stores;
  private Company company1;
  private Company company2;
  private ShardStrategy shardStrategy;
  private IShardResolutionStrategy shardResolution;

  private void executeOnShardedStore(Action1<ShardedDocumentStore> shardedAction) {
    try (IDocumentStore store1 = createStore();
      IDocumentStore store2 = createStore()) {

      company1 = new Company();
      company1.setName("company1");
      company2 = new Company();
      company2.setName("company2");

      stores = new LinkedHashMap<>();
      stores.put("Shard1", store1);
      stores.put("Shard2", store2);

      shardResolution = Mockito.mock(IShardResolutionStrategy.class);
      when(shardResolution.potentialShardsFor(any(ShardRequestData.class))).thenReturn(null);
      when(shardResolution.generateShardIdFor(eq(company1), any(ITransactionalDocumentSession.class))).thenReturn("Shard1");
      when(shardResolution.generateShardIdFor(eq(company2), any(ITransactionalDocumentSession.class))).thenReturn("Shard2");

      when(shardResolution.metadataShardIdFor(eq(company1))).thenReturn("Shard1");
      when(shardResolution.metadataShardIdFor(eq(company2))).thenReturn("Shard2");

      shardStrategy = new ShardStrategy(stores);
      shardStrategy.setShardResolutionStrategy(shardResolution);


      try (ShardedDocumentStore store = new ShardedDocumentStore(shardStrategy)) {
        store.initialize();
        shardedAction.apply(store);
      }
    }
  }

  @Test
  public void canOverrideTheShardIdGeneration() {
    executeOnShardedStore(new Action1<ShardedDocumentStore>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public void apply(ShardedDocumentStore documentStore) {
        for (IDocumentStore shard: stores.values()) {
          shard.getConventions().setDocumentKeyGenerator(new DocumentKeyGenerator() {
            @Override
            public String generate(String dbName, IDatabaseCommands dbCommands, Object c) {
              return ((Company)c).getName();
            }
          });
        }

        try (IDocumentSession session = documentStore.openSession()) {
          session.store(company1);
          session.store(company2);
          assertEquals("Shard1/companies/1", company1.getId());
          assertEquals("Shard2/companies/2", company2.getId());
        }
      }
    });
  }

  @Test
  public void canQueryUsingInt() {
    executeOnShardedStore(new Action1<ShardedDocumentStore>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public void apply(ShardedDocumentStore _) {
        shardStrategy.setShardAccessStrategy(new SequentialShardAccessStrategy());
        try (ShardedDocumentStore documentStore = new ShardedDocumentStore(shardStrategy)) {
          documentStore.initialize();

          try (IDocumentSession session = documentStore.openSession()) {
            session.load(Company.class, Integer.valueOf(1));
          }
        }
      }
    });
  }

  @Test
  public void canInsertIntoTwoShardedServers() {
    executeOnShardedStore(new Action1<ShardedDocumentStore>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public void apply(ShardedDocumentStore documentStore) {
        try (IDocumentSession session = documentStore.openSession()) {
          session.store(company1);
          session.store(company2);
          session.saveChanges();
        }
      }
    });
  }

  @Test
  public void canGetSingleEntityFromCorrectShardedServer() {
    executeOnShardedStore(new Action1<ShardedDocumentStore>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public void apply(ShardedDocumentStore documentStore) {
        try (IDocumentSession session = documentStore.openSession()) {
          // store item that goes in 2nd shard
          session.store(company2);
          session.saveChanges();

          // get it, should automagically retrieve from 2nd shard
          when(shardResolution.potentialShardsFor(any(ShardRequestData.class))).thenReturn(Arrays.asList("Shard2"));
          Company loadedCompany = session.load(Company.class, company2.getId());

          assertNotNull(loadedCompany);
          assertEquals(company2.getName(), loadedCompany.getName());
        }
      }
    });
  }

  @Test
  public void canGetSingleEntityFromCorrectShardedServerWhenLocationIsUnknown() {

    executeOnShardedStore(new Action1<ShardedDocumentStore>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public void apply(ShardedDocumentStore documentStore) {
        shardStrategy.setShardAccessStrategy(new SequentialShardAccessStrategy());
        try (IDocumentSession session = documentStore.openSession()) {
          //store item that goes in 2nd shard
          session.store(company2);
          session.saveChanges();

          //get it, should try all shards and find ti
          when(shardResolution.potentialShardsFor(any(ShardRequestData.class))).thenReturn(null);
          Company loadedCompany = session.load(Company.class, company2.getId());
          assertNotNull(loadedCompany);
          assertEquals(company2.getName(), loadedCompany.getName());
        }
      }
    });
  }

  @Test
  public void canGetAllShardedEntities() {
    executeOnShardedStore(new Action1<ShardedDocumentStore>() {

      @SuppressWarnings("synthetic-access")
      @Override
      public void apply(ShardedDocumentStore documentStore) {
        //get them in simple single threaded sequence for this test
        shardStrategy.setShardAccessStrategy(new SequentialShardAccessStrategy());
        try (IDocumentSession session = documentStore.openSession()) {
          //store 2 items in 2 shards
          session.store(company1);
          session.store(company2);

          session.saveChanges();

          //get all, should automagically retrieve from each shard
          List<Company> allCompanies = session.advanced().documentQuery(Company.class)
            .waitForNonStaleResults()
            .toList();

          assertNotNull(allCompanies);
          assertEquals(company1.getName(), allCompanies.get(0).getName());
          assertEquals(company2.getName(), allCompanies.get(1).getName());
        }
      }
    });
  }
}
