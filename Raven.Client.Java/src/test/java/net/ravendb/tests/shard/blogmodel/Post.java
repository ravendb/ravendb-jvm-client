package net.ravendb.tests.shard.blogmodel;

import java.util.Date;

import com.mysema.query.annotations.QueryEntity;

@QueryEntity
public class Post {
  private String id;
  private String title;
  private String content;
  private String blogId;
  private String userId;
  private int votesUpCount;
  private Date publishAt;

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

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getBlogId() {
    return blogId;
  }

  public void setBlogId(String blogId) {
    this.blogId = blogId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public int getVotesUpCount() {
    return votesUpCount;
  }

  public void setVotesUpCount(int votesUpCount) {
    this.votesUpCount = votesUpCount;
  }

  public Date getPublishAt() {
    return publishAt;
  }

  public void setPublishAt(Date publishAt) {
    this.publishAt = publishAt;
  }

}
