package net.ravendb.core.utils.entities;

import java.util.List;

import com.mysema.query.annotations.QueryEntity;

@QueryEntity
public class Post {
  private String id;
  private String title;
  private String desc;
  private List<Post> comments;
  private String[] attachmentIds;


  public Post(String id, String title, String desc) {
    super();
    this.id = id;
    this.title = title;
    this.desc = desc;
  }

  public Post() {
    super();
  }

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

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public List<Post> getComments() {
    return comments;
  }

  public void setComments(List<Post> comments) {
    this.comments = comments;
  }

  public String[] getAttachmentIds() {
    return attachmentIds;
  }

  public void setAttachmentIds(String[] attachmentIds) {
    this.attachmentIds = attachmentIds;
  }

}
