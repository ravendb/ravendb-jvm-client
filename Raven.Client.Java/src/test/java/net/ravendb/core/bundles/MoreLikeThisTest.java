package net.ravendb.core.bundles;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Comparator;

import org.junit.Test;

import net.ravendb.abstractions.data.MoreLikeThisQuery;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.RemoteClientTest;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.core.utils.entities.Post;
import net.ravendb.core.utils.indexes.Posts_ByTitleAndContent;


public class MoreLikeThisTest extends RemoteClientTest {

  @SuppressWarnings("boxing")
  @Test
  public void canUseBasicMoreLikeThis() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      Posts_ByTitleAndContent index = new Posts_ByTitleAndContent();
      index.execute(store);

      useFiddler(store);

      try (IDocumentSession session = store.openSession()) {
        session.store(new Post("posts/1", "doduck", "prototype"));
        session.store(new Post("posts/2", "doduck", "prototype your idea"));
        session.store(new Post("posts/3", "doduck", "love programming"));
        session.store(new Post("posts/4", "We do", "prototype"));
        session.store(new Post("posts/5", "We love", "challange"));
        session.saveChanges();

        waitForNonStaleIndexes(store.getDatabaseCommands());

        MoreLikeThisQuery query = new MoreLikeThisQuery();
        query.setDocumentId("posts/1");
        query.setMinimumDocumentFrequency(1);
        query.setMinimumTermFrequency(0);
        Post[] list = session.advanced().moreLikeThis(Post.class, Posts_ByTitleAndContent.class, query);
        assertEquals(3, list.length);

        Arrays.sort(list, new Comparator<Post>() {
          @Override
          public int compare(Post o1, Post o2) {
            int c1 = o1.getTitle().toLowerCase().compareTo(o2.getTitle().toLowerCase());
            if (c1 != 0) {
              return c1;
            }
            return o1.getDesc().toLowerCase().compareTo(o2.getDesc().toLowerCase());
          }
        });

        assertEquals("doduck", list[0].getTitle());
        assertEquals("love programming", list[0].getDesc());

        assertEquals("doduck", list[1].getTitle());
        assertEquals("prototype your idea", list[1].getDesc());

        assertEquals("We do", list[2].getTitle());
        assertEquals("prototype", list[2].getDesc());
      }
    }
  }
}

