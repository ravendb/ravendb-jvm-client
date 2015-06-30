package net.ravendb.tests.shard.blogmodel;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.abstractions.indexing.FieldIndexing;
import net.ravendb.abstractions.indexing.SortOptions;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.document.DocumentQueryCustomizationFactory;
import net.ravendb.client.indexes.AbstractIndexCreationTask;
import net.ravendb.client.shard.ShardReduceFunction;
import net.ravendb.tests.shard.blogmodel.CanMapReduceTest.TotalVotesUp.ReduceResult;

import org.junit.Test;

import com.mysema.query.annotations.QueryEntity;


public class CanMapReduceTest extends ShardingScenario {

  @Test
  public void canDoMapReduceOnAllShards() {
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      Post post1 = new Post();
      post1.setTitle("Item 1");
      post1.setVotesUpCount(2);

      Post post2 = new Post();
      post2.setTitle("Item 2");
      post2.setVotesUpCount(3);

      Post post3 = new Post();
      post3.setTitle("Item 3");
      post3.setVotesUpCount(4);

      Post post4 = new Post();
      post4.setTitle("Item 4");
      post4.setVotesUpCount(1);

      session.store(post1);
      session.store(post2);
      session.store(post3);
      session.store(post4);
      session.saveChanges();
    }

    new TotalVotesUp().execute(shardedDocumentStore);

    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      ReduceResult posts = session.query(TotalVotesUp.ReduceResult.class, TotalVotesUp.class, new ShardReduceFunction() {
        @SuppressWarnings("synthetic-access")
        @Override
        public List<Object> apply(IndexQuery query, List<Object> result) {
          TotalVotesUp.ReduceResult finalResult = new ReduceResult();
          for (Object r: result) {
            TotalVotesUp.ReduceResult subResult = (TotalVotesUp.ReduceResult) r;
            finalResult.totalVotesUp += subResult.totalVotesUp;
          }
          return Arrays.<Object> asList(finalResult);
        }
      }).customize(new DocumentQueryCustomizationFactory().waitForNonStaleResultsAsOfNow())
        .single();
      assertEquals(10, posts.getTotalVotesUp());
    }
  }

  @Test
  public void canDoGroupByMapReduceOnAllShards() throws ParseException {
    try (IDocumentSession session = shardedDocumentStore.openSession()) {

      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      Post post1 = new Post();
      post1.setTitle("Item 1");
      post1.setPublishAt(sdf.parse("2012-03-14"));

      Post post2 = new Post();
      post2.setTitle("Item 2");
      post2.setPublishAt(sdf.parse("2012-03-14"));

      Post post3 = new Post();
      post3.setTitle("Item 3");
      post3.setPublishAt(sdf.parse("2012-03-15"));

      Post post4 = new Post();
      post4.setTitle("Item 4");
      post4.setPublishAt(sdf.parse("2012-03-15"));

      Post post5 = new Post();
      post5.setTitle("Item 5");
      post5.setPublishAt(sdf.parse("2012-03-15"));

      session.store(post1);
      session.store(post2);
      session.store(post3);
      session.store(post4);
      session.store(post5);
      session.saveChanges();
    }

    new TotalPostsPerDay().execute(shardedDocumentStore);
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      QCanMapReduceTest_TotalPostsPerDay_ReduceResult r = QCanMapReduceTest_TotalPostsPerDay_ReduceResult.reduceResult;
      List<TotalPostsPerDay.ReduceResult> posts = session.query(TotalPostsPerDay.ReduceResult.class, TotalPostsPerDay.class, new ShardReduceFunction() {
        @SuppressWarnings({"boxing", "hiding"})
        @Override
        public List<Object> apply(IndexQuery query, List<Object> result) {
          Map<Date, Integer> map = new LinkedHashMap<>();
          for (Object r: result) {
            TotalPostsPerDay.ReduceResult innerResult = (TotalPostsPerDay.ReduceResult) r;
            if (map.containsKey(innerResult.getPublishAt())) {
              Integer currentValue = map.get(innerResult.getPublishAt());
              map.put(innerResult.getPublishAt(), currentValue + innerResult.getCount());
            } else {
              map.put(innerResult.getPublishAt(), innerResult.getCount());
            }
          }

          List<Object> finalResult = new ArrayList<>();
          for (Map.Entry<Date, Integer> kvp: map.entrySet()) {
            TotalPostsPerDay.ReduceResult item = new TotalPostsPerDay.ReduceResult();
            item.setCount(kvp.getValue());
            item.setPublishAt(kvp.getKey());
            finalResult.add(item);
          }
          return finalResult;
        }
      })
        .customize(new DocumentQueryCustomizationFactory().waitForNonStaleResultsAsOfNow())
        .orderBy(r.publishAt.asc())
        .toList();

      assertEquals(2, posts.size());
      assertEquals(2, posts.get(0).getCount());
      assertEquals(3, posts.get(1).getCount());
    }
  }

  @Test
  public void canMapOnAllShards() {
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      List<Post> postsToCreate = new ArrayList<>();
      int[] votesCount = new int[] { 99, 100, 200, 4, 5, 9, 1, 46, 84, 14 };
      for (int i = 0; i < 10; i++) {
        Post post = new Post();
        postsToCreate.add(post);
        post.setTitle("Item " + (i + 1));
        post.setContent("Content Sample 1");
        post.setVotesUpCount(votesCount[i]);
        session.store(post);
      }
      session.saveChanges();
    }

    new PostSearch().execute(shardedDocumentStore);

    QCanMapReduceTest_PostSearch_Result p = QCanMapReduceTest_PostSearch_Result.result;

    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      List<Post> posts = session.query(PostSearch.Result.class, PostSearch.class)
        .customize(new DocumentQueryCustomizationFactory().waitForNonStaleResultsAsOfNow())
        .where(p.query.eq("Content Sample 1"))
        .orderBy(p.votesUpCount.desc())
        .take(2)
        .as(Post.class)
        .toList();

      assertEquals(2, posts.size());
      assertEquals("Item 3", posts.get(0).getTitle());
      assertEquals("Item 2", posts.get(1).getTitle());
    }
  }

  public static class TotalVotesUp extends AbstractIndexCreationTask {
    @QueryEntity
    public static class ReduceResult {
      private int totalVotesUp;

      public int getTotalVotesUp() {
        return totalVotesUp;
      }

      public void setTotalVotesUp(int totalVotesUp) {
        this.totalVotesUp = totalVotesUp;
      }
    }

    public TotalVotesUp() {
      map = "from post in docs.posts select new { TotalVotesUp = post.VotesUpCount }";
      reduce = "from r in results group r by \"constant\" into g select new { TotalVotesUp = g.Sum( x => x.TotalVotesUp)}";
    }
  }

  public static class TotalPostsPerDay extends AbstractIndexCreationTask {
    @QueryEntity
    public static class ReduceResult {
      private Date publishAt;
      private int count;

      public Date getPublishAt() {
        return publishAt;
      }
      public void setPublishAt(Date publishAt) {
        this.publishAt = publishAt;
      }
      public int getCount() {
        return count;
      }
      public void setCount(int count) {
        this.count = count;
      }
    }

    public TotalPostsPerDay() {
      map = "from post in docs.posts select new { post.PublishAt, Count = 1 }";
      reduce = "from result in results group result by result.PublishAt into g select new { Count = g.Sum(x => x.Count), PublishAt = g.Key} ";
    }
  }

  public static class PostSearch extends AbstractIndexCreationTask {
    @QueryEntity
    public static class Result {
      private String query;
      private int votesUpCount;

      public String getQuery() {
        return query;
      }
      public void setQuery(String query) {
        this.query = query;
      }
      public int getVotesUpCount() {
        return votesUpCount;
      }
      public void setVotesUpCount(int votesUpCount) {
        this.votesUpCount = votesUpCount;
      }
    }

    public PostSearch() {
      map = "from post in docs.posts select new { Query = new object[] { post.Title, post.Content }, post.VotesUpCount } ";

      QCanMapReduceTest_PostSearch_Result p = QCanMapReduceTest_PostSearch_Result.result;
      index(p.query, FieldIndexing.ANALYZED);
      sort(p.votesUpCount, SortOptions.INT);
    }
  }
}
