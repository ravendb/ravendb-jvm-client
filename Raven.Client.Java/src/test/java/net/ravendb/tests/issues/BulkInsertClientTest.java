package net.ravendb.tests.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.data.BulkInsertOptions;
import net.ravendb.abstractions.data.BulkOperationOptions;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.RemoteClientTest;
import net.ravendb.client.document.BulkInsertOperation;
import net.ravendb.client.document.DocumentStore;

import org.junit.Test;



public class BulkInsertClientTest extends RemoteClientTest {
  @Test
  public void canCreateAndDisposeUsingBulk() throws InterruptedException {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
        bulkInsert.setReport(new Action1<String>() {
          @Override
          public void apply(String msg) {
            System.out.println(msg);
          }
        });

        User user = new User();
        user.setName("Fitzchak");
        bulkInsert.store(user);
      }

      try (IDocumentSession session = store.openSession()) {
        User user = session.load(User.class, "users/1");
        assertNotNull(user);
        assertEquals("Fitzchak", user.getName());
      }
    }
  }

    @Test
    public void canHandleBulkInsertWithMultipleChunks() throws InterruptedException {
        try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
            BulkInsertOptions bulkOperationOptions = new BulkInsertOptions();
            bulkOperationOptions.getChunkedBulkInsertOptions().setMaxDocumentsPerChunk(2);
            try (BulkInsertOperation bulkInsert = store.bulkInsert(getDefaultDb(), bulkOperationOptions)) {

                for (int i = 0; i < 100; i++) {
                    User user = new User();
                    user.setName("Marcin");
                    bulkInsert.store(user);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                assertEquals(100, session.query(User.class).count());
            }
        }
    }

  @Test
  public void canAbortAndDisposeUsingBulk() throws InterruptedException {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
        bulkInsert.setReport(new Action1<String>() {
          @Override
          public void apply(String msg) {
            System.out.println(msg);
          }
        });

        User user = new User();
        user.setName("Fitzchak");
        bulkInsert.store(user);
        bulkInsert.abort();
        try {
          bulkInsert.store(user);
          fail();
        } catch (IllegalStateException e) {
          //ok
        }
      }
    }
  }

  public static class User {
    private String id;
    private String name;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

}
