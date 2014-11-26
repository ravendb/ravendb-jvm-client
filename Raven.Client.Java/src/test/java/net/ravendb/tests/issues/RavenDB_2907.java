package net.ravendb.tests.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.mysema.query.annotations.QueryEntity;

import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.RemoteClientTest;
import net.ravendb.client.document.DocumentStore;


public class RavenDB_2907 extends RemoteClientTest {
  @QueryEntity
  public static class Foo {
    private String id;
    private Map<String, String> barIdInKey;
    private Map<String, String> barIdInValue;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }


    public Map<String, String> getBarIdInKey() {
      return barIdInKey;
    }


    public void setBarIdInKey(Map<String, String> barIdInKey) {
      this.barIdInKey = barIdInKey;
    }


    public Map<String, String> getBarIdInValue() {
      return barIdInValue;
    }


    public void setBarIdInValue(Map<String, String> barIdInValue) {
      this.barIdInValue = barIdInValue;
    }

    protected Foo() {
    }

    public Foo(Bar[] bars) {
      barIdInKey = new HashMap<>();
      barIdInValue = new HashMap<>();
      for (Bar b : bars) {
        barIdInKey.put(b.getId(), "Some value");
        barIdInValue.put("Some key", b.getId());
      }
    }
  }


  @QueryEntity
  public static class Bar {
    private String id;
    private String title;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }


    public String getTitle() {
      return title;
    }


    public void setTitle(String title) {
      this.title = title;
    }
  }

  public static class FooAnBar {
    public Foo foo;
    public Bar bar;

    public FooAnBar(Foo foo, Bar bar) {
      super();
      this.foo = foo;
      this.bar = bar;
    }
  }

  private FooAnBar makeAndStoreEntities(IDocumentStore db) throws Exception {
    try (IDocumentSession session = db.openSession()) {
      Bar bar = new Bar();
      session.store(bar);
      Foo foo = new Foo(new Bar[] { bar });
      session.store(foo);
      session.saveChanges();
      return new FooAnBar(foo, bar);
    }
  }

  @Test
  public void can_include_dictionary_key() throws Exception {
    try (IDocumentStore db = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      FooAnBar entities = makeAndStoreEntities(db);
      IDocumentSession session = db.openSession();
      QRavenDB_2907_Foo f = QRavenDB_2907_Foo.foo;

      Foo loaded = session.include(f.barIdInKey.keys()).load(Foo.class, entities.foo.getId());
      assertNotNull(loaded);
      Bar bar = session.load(Bar.class, entities.bar.getId());
      assertNotNull(bar);
      assertEquals(1, session.advanced().getNumberOfRequests());
    }
  }

  @Test
  public void can_include_dictionary_value() throws Exception {
    try (IDocumentStore db = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      FooAnBar entities = makeAndStoreEntities(db);
      IDocumentSession session = db.openSession();
      QRavenDB_2907_Foo f = QRavenDB_2907_Foo.foo;
      Foo loaded = session.include(f.barIdInValue.values().select()).load(Foo.class, entities.foo.getId());
      assertNotNull(loaded);
      Bar bar = session.load(Bar.class, entities.bar.getId());
      assertNotNull(bar);
      assertEquals(1, session.advanced().getNumberOfRequests());
    }
  }


}
