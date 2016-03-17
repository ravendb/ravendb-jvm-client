package net.ravendb.tests.issues;

import com.mysema.query.annotations.QueryEntity;
import net.ravendb.abstractions.basic.Lazy;
import net.ravendb.abstractions.data.GetRequest;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.RemoteClientTest;
import net.ravendb.client.connection.profiling.ProfilingInformation;
import net.ravendb.client.document.DocumentSession;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.imports.json.JsonConvert;
import net.ravendb.tests.linq.QWhereClauseTest_Dog;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class RavenDB_2854Test extends RemoteClientTest {

    @QueryEntity
    public static class Dog {
        private boolean cute;

        public boolean isCute() {
            return cute;
        }

        public void setCute(boolean cute) {
            this.cute = cute;
        }
    }

    @Test
    public void canGetCountWithoutGettingAllTheData() {
        try (DocumentStore store = (DocumentStore) new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {

            QRavenDB_2854Test_Dog d = QRavenDB_2854Test_Dog.dog;

            store.initializeProfiling();
            UUID id;
            try (IDocumentSession s = store.openSession()) {
                id = ((DocumentSession) s).getId();

                int count = s.query(Dog.class).count(d.cute);
                Assert.assertEquals(0, count);
            }

            ProfilingInformation profiling = store.getProfilingInformationFor(id);
            Assert.assertEquals(1, profiling.getRequests().size());

            Assert.assertTrue(profiling.getRequests().get(0).getUrl().contains("pageSize=0"));
        }
    }

    @Test
    public void canGetCountWithoutGettingAllTheDataLazy() {
        try (DocumentStore store = (DocumentStore) new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
            QRavenDB_2854Test_Dog d = QRavenDB_2854Test_Dog.dog;

            store.initializeProfiling();
            UUID id;
            try (IDocumentSession s = store.openSession()) {
                id = ((DocumentSession) s).getId();


                Lazy<Integer> countCute = s.query(Dog.class).where(d.cute).countLazily();
                Lazy<Integer> countNotCute = s.query(Dog.class).where(d.cute.eq(false)).countLazily();
                Assert.assertEquals(Integer.valueOf(0), countNotCute.getValue());
                Assert.assertEquals(Integer.valueOf(0), countCute.getValue());
            }

            ProfilingInformation profiling = store.getProfilingInformationFor(id);
            Assert.assertEquals(1, profiling.getRequests().size());
            // multi get

            GetRequest[] getRequests = JsonConvert.deserializeObject(GetRequest[].class, profiling.getRequests().get(0).getPostedData());
            Assert.assertEquals(2, getRequests.length);

            Assert.assertTrue(getRequests[0].getQuery().contains("pageSize=0"));
            Assert.assertTrue(getRequests[1].getQuery().contains("pageSize=0"));
        }
    }
}