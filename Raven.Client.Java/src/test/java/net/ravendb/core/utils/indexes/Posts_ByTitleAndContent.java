package net.ravendb.core.utils.indexes;

import net.ravendb.abstractions.indexing.FieldStorage;
import net.ravendb.client.indexes.AbstractIndexCreationTask;
import net.ravendb.core.utils.entities.QPost;


public class Posts_ByTitleAndContent extends AbstractIndexCreationTask {
  public Posts_ByTitleAndContent() {
    map = "from post in docs.posts select new { post.Title, post.Desc }";
    QPost p = QPost.post;
    store(p.title, FieldStorage.YES);
    store(p.desc, FieldStorage.YES);

    analyze(p.title, "Lucene.Net.Analysis.SimpleAnalyzer");
    analyze(p.desc, "Lucene.Net.Analysis.SimpleAnalyzer");
  }
}
