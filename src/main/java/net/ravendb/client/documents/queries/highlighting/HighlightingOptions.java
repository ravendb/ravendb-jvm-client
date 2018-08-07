package net.ravendb.client.documents.queries.highlighting;

public class HighlightingOptions {

    private String groupKey;
    private String[] preTags;
    private String[] postTags;

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public String[] getPreTags() {
        return preTags;
    }

    public void setPreTags(String[] preTags) {
        this.preTags = preTags;
    }

    public String[] getPostTags() {
        return postTags;
    }

    public void setPostTags(String[] postTags) {
        this.postTags = postTags;
    }
}
