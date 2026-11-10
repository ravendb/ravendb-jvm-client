package net.ravendb.client.test.issues;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.SearchEngineType;
import net.ravendb.client.documents.indexes.counters.AbstractCountersIndexCreationTask;
import net.ravendb.client.documents.indexes.counters.AbstractJavaScriptCountersIndexCreationTask;
import net.ravendb.client.documents.indexes.counters.AbstractMultiMapCountersIndexCreationTask;
import net.ravendb.client.documents.indexes.timeSeries.AbstractJavaScriptTimeSeriesIndexCreationTask;
import net.ravendb.client.documents.indexes.timeSeries.AbstractMultiMapTimeSeriesIndexCreationTask;
import net.ravendb.client.documents.indexes.timeSeries.AbstractTimeSeriesIndexCreationTask;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_22477Test {

    private static final String SEARCH_ENGINE_KEY = Constants.Configuration.Indexes.INDEXING_STATIC_SEARCH_ENGINE_TYPE;

    public static class MyCounterIndex extends AbstractJavaScriptCountersIndexCreationTask {
        public MyCounterIndex() {
            setMaps(Collections.singleton("counters.map('Companies', 'HeartRate', function (counter) {\n" +
                    "return {\n" +
                    "    HeartBeat: counter.Value,\n" +
                    "    Name: counter.Name,\n" +
                    "    User: counter.DocumentId\n" +
                    "};\n" +
                    "})"));

            searchEngineType = SearchEngineType.CORAX;
            compoundField("HeartBeat", "Name");
        }
    }

    public static class AverageHeartRate extends AbstractCountersIndexCreationTask {
        public AverageHeartRate() {
            map = "from counter in counters.Users.HeartRate " +
                    "select new { HeartBeat = counter.Value, Count = 1, Name = counter.Name }";

            reduce = "from r in results " +
                    "group r by r.Name into g " +
                    "let sumHeartBeat = g.Sum(x => x.HeartBeat) " +
                    "let sumCount = g.Sum(x => x.Count) " +
                    "select new { HeartBeat = sumHeartBeat / sumCount, Name = g.Key, Count = sumCount }";

            searchEngineType = SearchEngineType.CORAX;
        }
    }

    public static class CounterMapReduceIndexForMultipleCounterGroups extends AbstractMultiMapCountersIndexCreationTask {
        public CounterMapReduceIndexForMultipleCounterGroups() {
            for (int i = 0; i < 100; i++) {
                addMap("from counter in counters.Users.Counter" + i + " select new { Value = counter.Value, Count = 1 }");
            }

            reduce = "from result in results " +
                    "group result by new { result.Value } into g " +
                    "select new { Value = g.Key.Value, Count = g.Sum(r => r.Count) }";

            searchEngineType = SearchEngineType.CORAX;
            compoundField("Value", "Count");
        }
    }

    public static class MyTsIndex extends AbstractJavaScriptTimeSeriesIndexCreationTask {
        public MyTsIndex() {
            setMaps(Collections.singleton("timeSeries.map('Companies', 'HeartRate', function (ts) {\n" +
                    "return ts.Entries.map(entry => ({\n" +
                    "        HeartBeat: entry.Values[0],\n" +
                    "        Date: new Date(entry.Timestamp.getFullYear(), entry.Timestamp.getMonth(), entry.Timestamp.getDate()),\n" +
                    "        User: ts.DocumentId\n" +
                    "    }));\n" +
                    "})"));

            searchEngineType = SearchEngineType.CORAX;
            compoundField("HeartBeat", "User");
        }
    }

    public static class MyTsIndex_Strong extends AbstractTimeSeriesIndexCreationTask {
        public MyTsIndex_Strong() {
            map = "from ts in timeSeries.Companies.HeartRate " +
                    "from entry in ts.Entries " +
                    "select new { HeartBeat = entry.Values[0], entry.Timestamp.Date, User = ts.DocumentId }";

            searchEngineType = SearchEngineType.CORAX;
        }
    }

    public static class TimeSeriesIndex extends AbstractMultiMapTimeSeriesIndexCreationTask {
        public TimeSeriesIndex() {
            String[] names = new String[] { "Heartrate", "Heartrate@By6Hours", "Heartrate@By1Day", "Heartrate@By30Minutes", "Heartrate@By1Hour" };
            for (String name : names) {
                addMap("from ts in timeSeries.Users[\"" + name + "\"] " +
                        "from entry in ts.Entries " +
                        "select new { Name = ts.Name, Value = entry.Value, Start = ts.Start, End = ts.End }");
            }

            searchEngineType = SearchEngineType.CORAX;
            compoundField("Name", "Value");
        }
    }

    @Test
    public void indexes_SearchEngineType_Is_Respected_Counters() {
        IndexDefinition indexDefinition1 = new MyCounterIndex().createIndexDefinition();
        assertThat(indexDefinition1.getConfiguration().get(SEARCH_ENGINE_KEY))
                .isEqualTo("Corax");
        assertCompoundFields(indexDefinition1, "HeartBeat", "Name");

        IndexDefinition indexDefinition2 = new AverageHeartRate().createIndexDefinition();
        assertThat(indexDefinition2.getConfiguration().get(SEARCH_ENGINE_KEY))
                .isEqualTo("Corax");

        IndexDefinition indexDefinition3 = new CounterMapReduceIndexForMultipleCounterGroups().createIndexDefinition();
        assertThat(indexDefinition3.getConfiguration().get(SEARCH_ENGINE_KEY))
                .isEqualTo("Corax");
        assertCompoundFields(indexDefinition3, "Value", "Count");
    }

    @Test
    public void indexes_SearchEngineType_Is_Respected_TimeSeries() {
        IndexDefinition indexDefinition1 = new MyTsIndex().createIndexDefinition();
        assertThat(indexDefinition1.getConfiguration().get(SEARCH_ENGINE_KEY))
                .isEqualTo("Corax");
        assertCompoundFields(indexDefinition1, "HeartBeat", "User");

        IndexDefinition indexDefinition2 = new MyTsIndex_Strong().createIndexDefinition();
        assertThat(indexDefinition2.getConfiguration().get(SEARCH_ENGINE_KEY))
                .isEqualTo("Corax");

        IndexDefinition indexDefinition3 = new TimeSeriesIndex().createIndexDefinition();
        assertThat(indexDefinition3.getConfiguration().get(SEARCH_ENGINE_KEY))
                .isEqualTo("Corax");
        assertCompoundFields(indexDefinition3, "Name", "Value");
    }

    private static void assertCompoundFields(IndexDefinition definition, String first, String second) {
        assertThat(definition.getCompoundFields())
                .hasSize(1);
        assertThat(definition.getCompoundFields().get(0))
                .containsExactly(first, second);
    }
}
