package net.ravendb.client.test.spatial;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.spatial.SpatialOptionsFactory;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BoundingBoxIndexTest extends RemoteTestBase {

    @Test
    public void boundingBoxTest() throws Exception {
        String polygon = "POLYGON ((0 0, 0 5, 1 5, 1 1, 5 1, 5 5, 6 5, 6 0, 0 0))";
        String rectangle1 = "2 2 4 4";
        String rectangle2 = "6 6 10 10";
        String rectangle3 = "0 0 6 6";

        try (IDocumentStore store = getDocumentStore()) {
            new BBoxIndex().execute(store);
            new QuadTreeIndex().execute(store);

            try (IDocumentSession session = store.openSession()) {
                SpatialDoc doc = new SpatialDoc();
                doc.setShape(polygon);
                session.store(doc);
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                int result = session.query(SpatialDoc.class)
                        .count();
                assertThat(result)
                        .isEqualTo(1);
            }

            try (IDocumentSession session = store.openSession()) {
                int result = session.query(SpatialDoc.class, BBoxIndex.class)
                        .spatial("shape", x -> x.intersects(rectangle1))
                        .count();

                assertThat(result)
                        .isEqualTo(1);
            }

            try (IDocumentSession session = store.openSession()) {
                int result = session.query(SpatialDoc.class, BBoxIndex.class)
                        .spatial("shape", x -> x.intersects(rectangle2))
                        .count();

                assertThat(result)
                        .isEqualTo(0);
            }

            try (IDocumentSession session = store.openSession()) {
                int result = session.query(SpatialDoc.class, BBoxIndex.class)
                        .spatial("shape", x -> x.disjoint(rectangle2))
                        .count();

                assertThat(result)
                        .isEqualTo(1);
            }

            try (IDocumentSession session = store.openSession()) {
                int result = session.query(SpatialDoc.class, BBoxIndex.class)
                        .spatial("shape", x -> x.within(rectangle3))
                        .count();

                assertThat(result)
                        .isEqualTo(1);
            }

            try (IDocumentSession session = store.openSession()) {
                int result = session.query(SpatialDoc.class, QuadTreeIndex.class)
                        .spatial("shape", x -> x.intersects(rectangle2))
                        .count();

                assertThat(result)
                        .isEqualTo(0);
            }

            try (IDocumentSession session = store.openSession()) {
                int result = session.query(SpatialDoc.class, QuadTreeIndex.class)
                        .spatial("shape", x -> x.intersects(rectangle1))
                        .count();

                assertThat(result)
                        .isEqualTo(0);
            }
        }
    }

    public static class SpatialDoc {
        private String id;
        private String shape;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getShape() {
            return shape;
        }

        public void setShape(String shape) {
            this.shape = shape;
        }
    }

    public static class BBoxIndex extends AbstractIndexCreationTask {
        public BBoxIndex() {
            map = "docs.SpatialDocs.Select(doc => new {\n" +
                    "    shape = this.CreateSpatialField(doc.shape)\n" +
                    "})";
            spatial("shape", x -> x.cartesian().boundingBoxIndex());
        }
    }

    public static class QuadTreeIndex extends AbstractIndexCreationTask {
        public QuadTreeIndex() {
            map = "docs.SpatialDocs.Select(doc => new {\n" +
                    "    shape = this.CreateSpatialField(doc.shape)\n" +
                    "})";
            spatial("shape", x -> x.cartesian().quadPrefixTreeIndex(6, new SpatialOptionsFactory.SpatialBounds(0, 0, 16, 16)));
        }
    }
}
