package net.ravendb.abstractions.data;

import static org.junit.Assert.assertEquals;
import net.ravendb.abstractions.indexing.FieldIndexing;
import net.ravendb.abstractions.indexing.SortOptions;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.document.FailoverBehavior;
import net.ravendb.client.document.FailoverBehaviorSet;
import net.ravendb.client.document.JsonSerializer;

import org.junit.Test;


public class SharpEnumTest {
  @Test
  public void testEnumReadWrite() {
    DocumentConvention convention = new DocumentConvention();
    convention.setSaveEnumsAsIntegers(false);
    JsonSerializer mapper = new JsonSerializer(convention);

    assertEquals("\"Short\"", mapper.serializeAsString(SortOptions.SHORT));
    assertEquals(SortOptions.SHORT, mapper.deserialize("\"Short\"", SortOptions.class));

    testDeserialization(mapper);
  }

  @Test
  public void testSaveEnumsAsIntegers() {
    DocumentConvention convention = new DocumentConvention();
    convention.setSaveEnumsAsIntegers(true);
    JsonSerializer serializer = convention.createSerializer();

    assertEquals("0", serializer.serializeAsString(SortOptions.NONE));
    assertEquals("7", serializer.serializeAsString(SortOptions.DOUBLE));
    assertEquals("0", serializer.serializeAsString(FieldIndexing.NO));
    assertEquals("2", serializer.serializeAsString(FieldIndexing.NOT_ANALYZED));
    assertEquals("0", serializer.serializeAsString(new FailoverBehaviorSet()));
    assertEquals("0", serializer.serializeAsString(new FailoverBehaviorSet(FailoverBehavior.FAIL_IMMEDIATELY)));
    assertEquals("1027", serializer.serializeAsString(new FailoverBehaviorSet(FailoverBehavior.ALLOW_READS_FROM_SECONDARIES_AND_WRITES_TO_SECONDARIES, FailoverBehavior.READ_FROM_ALL_SERVERS)));

    testDeserialization(serializer);

  }

  private void testDeserialization(JsonSerializer mapper) {
    assertEquals(SortOptions.NONE, mapper.deserialize("0", SortOptions.class));
    assertEquals(SortOptions.NONE, mapper.deserialize("\"None\"", SortOptions.class));

    assertEquals(SortOptions.DOUBLE, mapper.deserialize("7", SortOptions.class));
    assertEquals(SortOptions.DOUBLE, mapper.deserialize("\"Double\"", SortOptions.class));

    assertEquals(FieldIndexing.NO, mapper.deserialize("\"No\"", FieldIndexing.class));
    assertEquals(FieldIndexing.NO, mapper.deserialize("0", FieldIndexing.class));

    assertEquals(FieldIndexing.NOT_ANALYZED, mapper.deserialize("\"NotAnalyzed\"", FieldIndexing.class));
    assertEquals(FieldIndexing.NOT_ANALYZED, mapper.deserialize("2", FieldIndexing.class));

    assertEquals(new FailoverBehaviorSet(), mapper.deserialize("0", FailoverBehaviorSet.class));
    assertEquals(new FailoverBehaviorSet(), mapper.deserialize("\"\"", FailoverBehaviorSet.class));

    assertEquals(new FailoverBehaviorSet(FailoverBehavior.FAIL_IMMEDIATELY), mapper.deserialize("0", FailoverBehaviorSet.class));
    assertEquals(new FailoverBehaviorSet(FailoverBehavior.FAIL_IMMEDIATELY), mapper.deserialize("\"\"", FailoverBehaviorSet.class));

    assertEquals(new FailoverBehaviorSet(FailoverBehavior.ALLOW_READS_FROM_SECONDARIES_AND_WRITES_TO_SECONDARIES, FailoverBehavior.READ_FROM_ALL_SERVERS), mapper.deserialize("1027", FailoverBehaviorSet.class));
    assertEquals(new FailoverBehaviorSet(FailoverBehavior.ALLOW_READS_FROM_SECONDARIES_AND_WRITES_TO_SECONDARIES, FailoverBehavior.READ_FROM_ALL_SERVERS), mapper.deserialize("\"AllowReadsFromSecondaries, AllowReadsFromSecondariesAndWritesToSecondaries, ReadFromAllServers\"", FailoverBehaviorSet.class));
    assertEquals(new FailoverBehaviorSet(FailoverBehavior.ALLOW_READS_FROM_SECONDARIES_AND_WRITES_TO_SECONDARIES, FailoverBehavior.READ_FROM_ALL_SERVERS), mapper.deserialize("\"AllowReadsFromSecondariesAndWritesToSecondaries, ReadFromAllServers\"", FailoverBehaviorSet.class));
    assertEquals(new FailoverBehaviorSet(FailoverBehavior.READ_FROM_ALL_SERVERS), mapper.deserialize("\"ReadFromAllServers\"", FailoverBehaviorSet.class));

  }

  @Test
  public void testSaveEnumsAsStrings()  {
    DocumentConvention convention = new DocumentConvention();
    convention.setSaveEnumsAsIntegers(false);
    JsonSerializer serializer = convention.createSerializer();

    assertEquals("\"None\"", serializer.serializeAsString(SortOptions.NONE));
    assertEquals("\"Double\"", serializer.serializeAsString(SortOptions.DOUBLE));
    assertEquals("\"No\"", serializer.serializeAsString(FieldIndexing.NO));
    assertEquals("\"NotAnalyzed\"", serializer.serializeAsString(FieldIndexing.NOT_ANALYZED));
    assertEquals("\"FailImmediately\"", serializer.serializeAsString(new FailoverBehaviorSet()));
    assertEquals("\"FailImmediately\"", serializer.serializeAsString(new FailoverBehaviorSet(FailoverBehavior.FAIL_IMMEDIATELY)));
    assertEquals("\"AllowReadsFromSecondariesAndWritesToSecondaries, ReadFromAllServers\"", serializer.serializeAsString(new FailoverBehaviorSet(FailoverBehavior.ALLOW_READS_FROM_SECONDARIES_AND_WRITES_TO_SECONDARIES, FailoverBehavior.READ_FROM_ALL_SERVERS)));

    testDeserialization(serializer);

  }

}
