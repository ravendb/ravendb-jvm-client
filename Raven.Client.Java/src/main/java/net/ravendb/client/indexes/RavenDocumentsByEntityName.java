package net.ravendb.client.indexes;

import java.util.HashMap;
import java.util.Map;

import net.ravendb.abstractions.indexing.FieldIndexing;
import net.ravendb.abstractions.indexing.IndexDefinition;
import net.ravendb.abstractions.indexing.IndexLockMode;
import net.ravendb.abstractions.indexing.SortOptions;


/**
 * Create an index that allows to tag entities by their entity name
 */
public class RavenDocumentsByEntityName extends AbstractIndexCreationTask {

  @Override
  public boolean isMapReduce() {
    return false;
  }

  @Override
  public String getIndexName() {
    return "Raven/DocumentsByEntityName";
  }

  @Override
  public IndexDefinition createIndexDefinition() {
    IndexDefinition def = new IndexDefinition();
    def.setMap("from doc in docs select new { Tag = doc[\"@metadata\"][\"Raven-Entity-Name\"], "
      + "LastModified = (DateTime)doc[\"@metadata\"][\"Last-Modified\"], "
      + "LastModifiedTicks = ((DateTime)doc[\"@metadata\"][\"Last-Modified\"]).Ticks };");

    Map<String, FieldIndexing> indexes = new HashMap<>();
    indexes.put("Tag", FieldIndexing.NOT_ANALYZED);
    indexes.put("LastModified", FieldIndexing.NOT_ANALYZED);
    indexes.put("LastModifiedTicks", FieldIndexing.NOT_ANALYZED);
    def.setIndexes(indexes);

    Map<String, SortOptions> sortOptions = new HashMap<>();
    sortOptions.put("LastModified", SortOptions.STRING);
    sortOptions.put("LastModifiedTicks", SortOptions.LONG);
    def.setSortOptions(sortOptions);

    def.setDisableInMemoryIndexing(true);
    def.setLockMode(IndexLockMode.LOCKED_IGNORE);

    return def;
  }



}
