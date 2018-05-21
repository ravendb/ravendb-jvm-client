package net.ravendb.client.documents.queries.moreLikeThis;

public abstract class MoreLikeThisBase {

    protected MoreLikeThisOptions options;

    public MoreLikeThisOptions getOptions() {
        return options;
    }

    public void setOptions(MoreLikeThisOptions options) {
        this.options = options;
    }

}
