package net.ravendb.tests.shard.blogmodel;

import com.mysema.query.annotations.QueryEntity;

@QueryEntity
public class Blog {
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
