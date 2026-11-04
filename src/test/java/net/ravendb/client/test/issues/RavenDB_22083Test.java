package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.exceptions.documents.compilation.IndexCompilationException;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@EnableOnServer(thresholdVersion = "7.2.7")
public class RavenDB_22083Test extends RemoteTestBase {

    private static IndexCompilationException putIndex(IDocumentStore store, IndexDefinition definition) {
        Throwable thrown = null;
        try {
            store.maintenance().send(new PutIndexesOperation(definition));
        } catch (Throwable t) {
            thrown = t;
        }

        assertThat(thrown).isInstanceOf(IndexCompilationException.class);
        return (IndexCompilationException) thrown;
    }

    private static IndexDefinition definition(String name, String map) {
        IndexDefinition definition = new IndexDefinition();
        definition.setName(name);
        definition.setMaps(Collections.singleton(map));
        return definition;
    }

    @Test
    public void indexDefinitionSentAsTextWithConstantArrayAsOuterSourceThrowsIndexCompilationException() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IndexCompilationException e = putIndex(store, definition("CandlesByTimeframeFromText",
                    "from x in new[] { new { Timeframe = \"1m\", Interval = 60000L }, new { Timeframe = \"5m\", Interval = 300000L } }\n" +
                    "from candle in docs.Candles\n" +
                    "select new { Ref = $\"{candle.Time / x.Interval}\", Volume = candle.Volume, Timeframe = x.Timeframe }"));

            assertThat(e.getMessage())
                    .contains("Failed to compile index 'CandlesByTimeframeFromText'")
                    .contains("must start its enumeration from 'docs'");
            assertThat(e.getIndexDefinitionProperty()).isEqualTo("Maps");
        }
    }

    @Test
    public void indexDefinitionSentAsTextNotRootedInDocumentsSourceThrowsIndexCompilationException() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IndexCompilationException e = putIndex(store, definition("CandlesFromMethodSyntaxText",
                    "new[] { 60000L, 300000L }.SelectMany(t => docs.Candles, (t, candle) => new { Ref = candle.Time / t, Volume = candle.Volume })"));

            assertThat(e.getIndexDefinitionProperty()).isEqualTo("Maps");
            assertThat(e.getMessage())
                    .contains("compiled as JavaScript")
                    .contains("a C# map must start its enumeration from one of these sources");
        }
    }

    @Test
    public void javaScriptIndexWithInvalidMapThrowsIndexCompilationException() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IndexCompilationException e = putIndex(store, definition("BrokenJsMap",
                    "map('Candles', function (c) { return { Volume: c.Volume }; )"));

            assertThat(e.getIndexDefinitionProperty()).isEqualTo("Maps");
        }
    }

    @Test
    public void javaScriptIndexWithInvalidReduceThrowsIndexCompilationException() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IndexDefinition definition = definition("BrokenJsReduce", "map('Candles', function (c) { return { Volume: c.Volume }; })");
            definition.setReduce("groupBy(x => x.Volume).aggregate(g => { return { Volume: g.values.reduce((a, b) => a + b.Volume, 0) }; )");

            IndexCompilationException e = putIndex(store, definition);

            assertThat(e.getIndexDefinitionProperty()).isEqualTo("Reduce");
        }
    }
}
