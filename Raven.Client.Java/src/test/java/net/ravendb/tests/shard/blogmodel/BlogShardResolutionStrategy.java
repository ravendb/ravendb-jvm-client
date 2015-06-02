package net.ravendb.tests.shard.blogmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.ravendb.client.ITransactionalDocumentSession;
import net.ravendb.client.shard.IShardResolutionStrategy;
import net.ravendb.client.shard.ShardRequestData;
import net.ravendb.tests.shard.blogmodel.CanMapReduceTest.TotalPostsPerDay;
import net.ravendb.tests.shard.blogmodel.CanMapReduceTest.TotalVotesUp;


public class BlogShardResolutionStrategy implements IShardResolutionStrategy {

  private final int numberOfShardsForPosts;
  private AtomicInteger currentNewShardId = new AtomicInteger();

  public BlogShardResolutionStrategy(int numberOfShardsForPosts) {
    this.numberOfShardsForPosts = numberOfShardsForPosts;
  }

  @Override
  public String generateShardIdFor(Object entity, ITransactionalDocumentSession sessionMetadata) {
    return getShardIdFromObjectType(entity);
  }

  @Override
  public String metadataShardIdFor(Object entity) {
    return getShardIdFromObjectType(entity, true);
  }

  private String getShardIdFromObjectType(Object instance) {
    return getShardIdFromObjectType(instance, false);
  }

  @SuppressWarnings("boxing")
  private String getShardIdFromObjectType(Object instance, boolean requiredMaster) {
    if (instance instanceof User) {
      return "Users";
    }
    if (instance instanceof Blog) {
      return "Blogs";
    }
    if (instance instanceof Post) {
      if (requiredMaster) {
        return "Posts01";
      }
      int nextPostShardId = currentNewShardId.incrementAndGet() % numberOfShardsForPosts + 1;
      return String.format("Posts%02d", nextPostShardId);
    }

    throw new IllegalArgumentException("Cannot get shard id for " + instance + " because it is not a User, Blog or Post");
  }

  @SuppressWarnings("boxing")
  @Override
  public List<String> potentialShardsFor(ShardRequestData requestData) {
    if (User.class.equals(requestData.getEntityType())) {
      return Arrays.asList("Users");
    }
    if (Blog.class.equals(requestData.getEntityType())) {
      return Arrays.asList("Blogs");
    }
    if (Post.class.equals(requestData.getEntityType())
      || TotalVotesUp.ReduceResult.class.equals(requestData.getEntityType())
      || TotalPostsPerDay.ReduceResult.class.equals(requestData.getEntityType())) {
      List<String> result = new ArrayList<>();
      for (int i = 0; i < numberOfShardsForPosts; i++) {
        result.add(String.format("Posts%02d", i + 1));
      }
      return result;
    }
    throw new IllegalArgumentException("Cannot get shard id for " + requestData.getEntityType() + " because it is not a User, Blog or Post");
  }
}
