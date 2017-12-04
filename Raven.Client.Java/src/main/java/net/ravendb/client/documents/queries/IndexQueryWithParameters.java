package net.ravendb.client.documents.queries;

public abstract class IndexQueryWithParameters<T> extends IndexQueryBase<T> {

    private boolean skipDuplicateChecking;

    private boolean explainScores;

    //TBD private boolean showTimings;

    /**
     * Allow to skip duplicate checking during queries
     */
    public boolean isSkipDuplicateChecking() {
        return skipDuplicateChecking;
    }

    /**
     * Allow to skip duplicate checking during queries
     */
    public void setSkipDuplicateChecking(boolean skipDuplicateChecking) {
        this.skipDuplicateChecking = skipDuplicateChecking;
    }

    /**
     * Whatever a query result should contain an explanation about how docs scored against query
     */
    public boolean isExplainScores() {
        return explainScores;
    }

    /**
     * Whatever a query result should contain an explanation about how docs scored against query
     */
    public void setExplainScores(boolean explainScores) {
        this.explainScores = explainScores;
    }

    /**
     * Indicates if detailed timings should be calculated for various query parts (Lucene search, loading documents, transforming results). Default: false
     */
    /* TBD public boolean isShowTimings() {
        return showTimings;
    }*/

    /**
     * Indicates if detailed timings should be calculated for various query parts (Lucene search, loading documents, transforming results). Default: false
     */
    /* TBD
    public void setShowTimings(boolean showTimings) {
        this.showTimings = showTimings;
    }
    */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        IndexQueryWithParameters<?> that = (IndexQueryWithParameters<?>) o;

        if (skipDuplicateChecking != that.skipDuplicateChecking) return false;
        return true;
        //TBD return showTimings == that.showTimings;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (skipDuplicateChecking ? 1 : 0);
        //TBD result = 31 * result + (showTimings ? 1 : 0);
        return result;
    }
}
