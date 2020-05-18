package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.indexes.IndexDefinitionHelper;
import net.ravendb.client.documents.indexes.IndexSourceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_13100Test extends RemoteTestBase {
    @Test
    public void canDetectTimeSeriesIndexSourceMethodSyntax() throws Exception {
        String map = "timeSeries.Companies.SelectMany(ts => ts.Entries, (ts, entry) => new {" +
                "   HeartBeat = entry.Values[0], " +
                "   Date = entry.Timestamp.Date, " +
                "   User = ts.DocumentId " +
                "});";

        assertThat(IndexDefinitionHelper.detectStaticIndexSourceType(map))
                .isEqualTo(IndexSourceType.TIME_SERIES);
    }

    @Test
    public void canDetectDocumentsIndexSourceMethodSyntax() throws Exception {
        String map = "docs.Users.OrderBy(user => user.Id).Select(user => new { user.Name })";

        assertThat(IndexDefinitionHelper.detectStaticIndexSourceType(map))
                .isEqualTo(IndexSourceType.DOCUMENTS);
    }

    @Test
    public void canDetectTimeSeriesIndexSourceLinqSyntaxAllTs() {
        String map = "from ts in timeSeries";
        assertThat(IndexDefinitionHelper.detectStaticIndexSourceType(map))
                .isEqualTo(IndexSourceType.TIME_SERIES);
    }

    @Test
    public void canDetectTimeSeriesIndexSourceLinqSyntaxSingleTs() throws Exception {
        String map = "from ts in timeSeries.Users";
        assertThat(IndexDefinitionHelper.detectStaticIndexSourceType(map))
                .isEqualTo(IndexSourceType.TIME_SERIES);
    }

    @Test
    public void canDetectTimeSeriesIndexSourceLinqSyntaxCanStripWhiteSpace() throws Exception {
        String map = "\t\t  \t from    ts  \t \t in  \t \t timeSeries.Users";
        assertThat(IndexDefinitionHelper.detectStaticIndexSourceType(map))
                .isEqualTo(IndexSourceType.TIME_SERIES);
    }
}
