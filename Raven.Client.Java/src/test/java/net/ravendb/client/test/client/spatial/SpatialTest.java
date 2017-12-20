package net.ravendb.client.test.client.spatial;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.FieldStorage;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.primitives.Reference;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SpatialTest extends RemoteTestBase {

    public static class MyDocumentItem {
        private Date date;
        private Double latitude;
        private Double longitude;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }
    }

    public static class MyDocument {
        private String id;
        private List<MyDocumentItem> items;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<MyDocumentItem> getItems() {
            return items;
        }

        public void setItems(List<MyDocumentItem> items) {
            this.items = items;
        }
    }

    public static class MyProjection {
        private String id;
        private Date date;
        private double latitude;
        private double longitude;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
    }

    public static class MyIndex extends AbstractIndexCreationTask {
        public MyIndex() {
            map = "docs.MyDocuments.SelectMany(doc => doc.items, (doc, item) => new {\n" +
                    "    doc = doc,\n" +
                    "    item = item\n" +
                    "}).Select(this0 => new {\n" +
                    "    this0 = this0,\n" +
                    "    lat = ((double)(this0.item.latitude ?? 0))\n" +
                    "}).Select(this1 => new {\n" +
                    "    this1 = this1,\n" +
                    "    lng = ((double)(this1.this0.item.longitude ?? 0))\n" +
                    "}).Select(this2 => new {\n" +
                    "    id = Id(this2.this1.this0.doc),\n" +
                    "    date = this2.this1.this0.item.date,\n" +
                    "    latitude = this2.this1.lat,\n" +
                    "    longitude = this2.lng,\n" +
                    "    coordinates = this.CreateSpatialField(((double ? ) this2.this1.lat), ((double ? ) this2.lng))\n" +
                    "})";
            store("id", FieldStorage.YES);
            store("date", FieldStorage.YES);

            store("latitude", FieldStorage.YES);
            store("longitude", FieldStorage.YES);

        }
    }

    @Test
    public void weirdSpatialResults() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                MyDocument myDocument = new MyDocument();
                myDocument.setId("First");

                MyDocumentItem myDocumentItem = new MyDocumentItem();
                myDocumentItem.setDate(new Date());
                myDocumentItem.setLatitude(10.0);
                myDocumentItem.setLongitude(10.0);

                myDocument.setItems(Collections.singletonList(myDocumentItem));

                session.store(myDocument);
                session.saveChanges();
            }

            new MyIndex().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Reference<QueryStatistics> statsRef = new Reference<>();

                List<MyProjection> result = session.advanced().documentQuery(MyDocument.class, MyIndex.class)
                        .waitForNonStaleResults()
                        .withinRadiusOf("coordinates", 0, 12.3456789f, 12.3456789f)
                        .statistics(statsRef)
                        .selectFields(MyProjection.class, "id", "latitude", "longitude")
                        .take(50)
                        .toList();

                assertThat(statsRef.value.getTotalResults())
                        .isEqualTo(0);

                assertThat(result)
                        .hasSize(0);
            }
        }
    }

    @Test
    public void matchSpatialResults() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                MyDocument myDocument = new MyDocument();
                myDocument.setId("First");

                MyDocumentItem myDocumentItem = new MyDocumentItem();
                myDocumentItem.setDate(new Date());
                myDocumentItem.setLatitude(10.0);
                myDocumentItem.setLongitude(10.0);

                myDocument.setItems(Collections.singletonList(myDocumentItem));

                session.store(myDocument);
                session.saveChanges();
            }

            new MyIndex().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Reference<QueryStatistics> statsRef = new Reference<>();

                List<MyProjection> result = session.advanced().documentQuery(MyDocument.class, MyIndex.class)
                        .waitForNonStaleResults()
                        .withinRadiusOf("coordinates", 1, 10, 10)
                        .statistics(statsRef)
                        .selectFields(MyProjection.class, "id", "latitude", "longitude")
                        .take(50)
                        .toList();

                assertThat(statsRef.value.getTotalResults())
                        .isEqualTo(1);

                assertThat(result)
                        .hasSize(1);
            }
        }
    }
}
