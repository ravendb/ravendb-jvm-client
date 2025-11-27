package net.ravendb.client.documents.queries.moreLikeThis;

/**
 * {@inheritDoc}
 * @see IMoreLikeThisOperations
 */
public abstract class MoreLikeThisBase {
    /**
     * {@inheritDoc}
     * @see MoreLikeThisOptions
     */
    protected MoreLikeThisOptions options;

    public MoreLikeThisOptions getOptions() {
        return options;
    }

    public void setOptions(MoreLikeThisOptions options) {
        this.options = options;
    }

}
