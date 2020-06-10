package net.ravendb.client.test.client.indexing.timeSeries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.timeSeries.TimeSeriesIndexDefinition;
import net.ravendb.client.documents.operations.indexes.GetTermsOperation;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class BasicTimeSeriesIndexes_MixedSyntaxTest extends RemoteTestBase {

    @Test
    public void basicMapIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date now1 = new Date();

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                session.store(company, "companies/1");
                session.timeSeriesFor(company, "HeartRate")
                        .append(now1, 7, "tag");

                session.saveChanges();
            }

            TimeSeriesIndexDefinition timeSeriesIndexDefinition = new TimeSeriesIndexDefinition();
            timeSeriesIndexDefinition.setName("MyTsIndex");
            timeSeriesIndexDefinition.setMaps(Collections.singleton("from ts in timeSeries.Companies.HeartRate.Where(x => true) " +
                    "from entry in ts.Entries " +
                    "select new { " +
                    "   heartBeat = entry.Values[0], " +
                    "   date = entry.Timestamp.Date, " +
                    "   user = ts.DocumentId " +
                    "}"));

            store.maintenance().send(new PutIndexesOperation(timeSeriesIndexDefinition));

            waitForIndexing(store);

            String[] terms = store.maintenance().send(new GetTermsOperation("MyTsIndex", "heartBeat", null));
            assertThat(terms)
                    .hasSize(1);
            assertThat(terms)
                    .contains("7");
        }
    }
}
