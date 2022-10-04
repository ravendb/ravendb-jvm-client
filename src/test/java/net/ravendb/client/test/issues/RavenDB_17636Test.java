package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.FieldIndexing;
import net.ravendb.client.documents.indexes.FieldStorage;
import net.ravendb.client.documents.queries.facets.Facet;
import net.ravendb.client.documents.queries.facets.FacetOptions;
import net.ravendb.client.documents.queries.facets.FacetSetup;
import net.ravendb.client.documents.queries.facets.FacetTermSortMode;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.exceptions.InvalidQueryException;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.Tuple;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_17636Test extends RemoteTestBase {

    @Test
    public void canUseFilterWithCollectionQuery() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            List<Tuple<Employee, String>> data = getDatabaseItems();

            insert(store, data);

            Employee result;
            Reference<QueryStatistics> statsRef = new Reference<>();

            try (IDocumentSession session = store.openSession()) {
                result = session.advanced().rawQuery(Employee.class, "from Employees filter name = 'Jane'").singleOrDefault();
                assertThat(result.getName())
                        .isEqualTo("Jane");

                result = session.query(Employee.class).filter(p -> p.equals("name", "Jane")).singleOrDefault();
                assertThat(result.getName())
                        .isEqualTo("Jane");
            }

            // scan limit

            try (IDocumentSession session = store.openSession()) {
                result = session.advanced()
                        .rawQuery(Employee.class, "from Employees filter name = 'Jane' filter_limit 1")
                        .statistics(statsRef)
                        .singleOrDefault();

                assertThat(result.getName())
                        .isEqualTo("Jane");
                assertThat(statsRef.value.getScannedResults())
                        .isEqualTo(1);

                result = session.query(Employee.class)
                        .filter(p -> p.equals("name", "Jane"), 1)
                        .statistics(statsRef)
                        .singleOrDefault();
                assertThat(result.getName())
                        .isEqualTo("Jane");
                assertThat(statsRef.value.getScannedResults())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void cannotUseFacetWithFilter() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            new BlogIndex().execute(store);

            Facet facet = new Facet();
            facet.setFieldName("tags");
            facet.setOptions(new FacetOptions());
            facet.getOptions().setTermSortMode(FacetTermSortMode.COUNT_DESC);

            List<Facet> facets = Arrays.asList(facet);

            try (IDocumentSession session = store.openSession()) {
                FacetSetup facetSetup = new FacetSetup();
                facetSetup.setFacets(facets);
                facetSetup.setId("facets/BlogFacets");
                session.store(facetSetup);

                BlogPost post1 = new BlogPost();
                post1.setTitle("my first blog");
                post1.setTags(Arrays.asList("news", "funny"));

                session.store(post1);

                BlogPost post2 = new BlogPost();
                post2.setTitle("my second blog");
                post2.setTags(Arrays.asList("lame", "news"));

                session.store(post2);
                session.saveChanges();
            }

            indexes.waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<BlogPost> q = session.query(BlogPost.class, BlogIndex.class)
                        .filter(p -> p.equals("tags", "news"));

                assertThatThrownBy(() -> q.aggregateUsing("facets/BlogFacets").execute())
                        .isExactlyInstanceOf(InvalidQueryException.class);
            }
        }
    }

    private List<Tuple<Employee, String>> getDatabaseItems() {
        Employee e1 = new Employee();
        e1.setName("Jane");
        e1.setActive(true);
        e1.setAge(20);

        Tuple<Employee, String> t1 = Tuple.create(e1, "emps/jane");

        Employee e2 = new Employee();
        e2.setName("Mark");
        e2.setActive(false);
        e2.setAge(33);

        Tuple<Employee, String> t2 = Tuple.create(e2, "emps/mark");

        Employee e3 = new Employee();
        e3.setName("Sandra");
        e3.setActive(true);
        e3.setAge(35);

        Tuple<Employee, String> t3 = Tuple.create(e3, "emps/sandra");


        return Arrays.asList(t1, t2, t3);
    }

    private void insert(DocumentStore store, List<Tuple<Employee, String>> data) {
        try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
            for (Tuple<Employee, String> datum : data) {
                bulkInsert.store(datum.first, datum.second);
            }
        }
    }

    public static class Employee {
        private String name;
        private String manager;
        private boolean active;
        private int age;
        private Location location;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getManager() {
            return manager;
        }

        public void setManager(String manager) {
            this.manager = manager;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }
    }

    public static class Location {
        private float latitude;
        private float longitude;

        public float getLatitude() {
            return latitude;
        }

        public void setLatitude(float latitude) {
            this.latitude = latitude;
        }

        public float getLongitude() {
            return longitude;
        }

        public void setLongitude(float longitude) {
            this.longitude = longitude;
        }
    }

    public static class BlogPost {
        private String title;
        private List<String> tags;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }
    public static class BlogIndex extends AbstractIndexCreationTask {
        public BlogIndex() {
            this.map = "from b in docs.Blogs select new { b.tags }";

            store("tags", FieldStorage.YES);
            index("tags", FieldIndexing.EXACT);
        }
    }

}
