package net.ravendb.tests.shard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import net.ravendb.abstractions.closure.Function2;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.shard.ParallelShardAccessStrategy;
import net.ravendb.client.shard.ShardRequestData;
import net.ravendb.samples.entities.Company;
import net.ravendb.tests.bundles.replication.ReplicationBase;


public class WhenUsingParallelAccessStrategyTest extends ReplicationBase {
  @Test
  public void nullResultIsNotAnException() {
    try (IDocumentStore store1 = createStore()) {
      try (IDocumentSession session = store1.openSession()) {
        try (ParallelShardAccessStrategy access= new ParallelShardAccessStrategy()) {
          Company[] results = access.apply(Company.class, Arrays.asList(store1.getDatabaseCommands()), new ShardRequestData(), new Function2<IDatabaseCommands, Integer, Company>() {
            @Override
            public Company apply(IDatabaseCommands x, Integer i) {
              return null;
            }
          });
          assertEquals(1, results.length);
          assertNull(results[0]);
        }
      }
    }
  }

  @Test
  public void executionExceptionsAreRethrown() {
    try (IDocumentStore store1 = createStore()) {
      try (IDocumentSession session = store1.openSession()) {
        try (ParallelShardAccessStrategy access = new ParallelShardAccessStrategy()) {
          try {
            access.apply(Object.class, Arrays.asList(store1.getDatabaseCommands()), new ShardRequestData(), new Function2<IDatabaseCommands, Integer, Object>() {
              @Override
              public Object apply(IDatabaseCommands x, Integer i) {
                throw new RuntimeException();
              }
            });
            fail();
          } catch (RuntimeException e) {
            //ok
          } catch (Exception e) {
            fail();
          }
        }
      }
    }
  }
}
