package net.ravendb.tests.issues;

import static org.junit.Assert.assertEquals;
import net.ravendb.abstractions.data.IndexStats;
import net.ravendb.abstractions.data.IndexStats.IndexingPriority;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.RemoteClientTest;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.client.indexes.AbstractIndexCreationTask;

import org.junit.Test;


public class RavenDB_3314Test extends RemoteClientTest {

  private static IndexStats findIndexByName(String name, IndexStats[] stats) {
    for (IndexStats stat: stats) {
      if (stat.getName().equals(name)) {
        return stat;
      }
    }
    return null;
  }

  @Test
  public void set_index_priority() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      SampleIndex index = new SampleIndex();
      index.setConventions(new DocumentConvention());
      index.execute(store);

      store.getDatabaseCommands().setIndexPriority("SampleIndex", IndexingPriority.NORMAL);

      IndexStats stats = findIndexByName("SampleIndex", store.getDatabaseCommands().getStatistics().getIndexes());
      assertEquals(IndexingPriority.NORMAL, stats.getPriority());

      store.getDatabaseCommands().setIndexPriority("SampleIndex", IndexingPriority.IDLE);
      stats = findIndexByName("SampleIndex", store.getDatabaseCommands().getStatistics().getIndexes());
      assertEquals(IndexingPriority.IDLE, stats.getPriority());

      store.getDatabaseCommands().setIndexPriority("SampleIndex", IndexingPriority.DISABLED);
      stats = findIndexByName("SampleIndex", store.getDatabaseCommands().getStatistics().getIndexes());
      assertEquals(IndexingPriority.DISABLED, stats.getPriority());

      store.getDatabaseCommands().setIndexPriority("SampleIndex", IndexingPriority.ABANDONED);
      stats = findIndexByName("SampleIndex", store.getDatabaseCommands().getStatistics().getIndexes());
      assertEquals(IndexingPriority.ABANDONED, stats.getPriority());

    }
  }

  @Test
  public void set_index_priority_through_index_definition() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      SampleIndex1 index1 = new SampleIndex1();
      index1.setConventions(new DocumentConvention());
      index1.execute(store);

      IndexStats stats = findIndexByName("SampleIndex1", store.getDatabaseCommands().getStatistics().getIndexes());
      assertEquals(IndexingPriority.ABANDONED, stats.getPriority());

      SampleIndex2 index2 = new SampleIndex2();
      index2.setConventions(new DocumentConvention());
      index2.execute(store);

      stats = findIndexByName("SampleIndex2", store.getDatabaseCommands().getStatistics().getIndexes());
      assertEquals(IndexingPriority.IDLE, stats.getPriority());

      SampleIndex3 index3 = new SampleIndex3();
      index3.setConventions(new DocumentConvention());
      index3.execute(store);

      stats = findIndexByName("SampleIndex3", store.getDatabaseCommands().getStatistics().getIndexes());
      assertEquals(IndexingPriority.DISABLED, stats.getPriority());

      SampleIndex4 index4 = new SampleIndex4();
      index4.setConventions(new DocumentConvention());
      index4.execute(store);

      stats = findIndexByName("SampleIndex4", store.getDatabaseCommands().getStatistics().getIndexes());
      assertEquals(IndexingPriority.NORMAL, stats.getPriority());
    }
  }

  public static class SampleIndex extends AbstractIndexCreationTask {
    public SampleIndex() {
      map = "from e in docs.Employees select new { e.Name, e.Address }";
    }
  }

  public static class SampleIndex1 extends AbstractIndexCreationTask {
    public SampleIndex1() {
      map = "from e in docs.Employees select new { e.Name, e.Address }";
      setPriority(IndexingPriority.ABANDONED);
    }
  }

  public static class SampleIndex2 extends AbstractIndexCreationTask {
    public SampleIndex2() {
      map = "from e in docs.Employees select new { e.Name, e.Address }";
      setPriority(IndexingPriority.IDLE);
    }
  }

  public static class SampleIndex3 extends AbstractIndexCreationTask {
    public SampleIndex3() {
      map = "from e in docs.Employees select new { e.Name, e.Address }";
      setPriority(IndexingPriority.DISABLED);
    }
  }

  public static class SampleIndex4 extends AbstractIndexCreationTask {
    public SampleIndex4() {
      map = "from e in docs.Employees select new { e.Name, e.Address }";
    }
  }

  public static class Employee {
    private String name;
    private String address;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
    }
  }

}
