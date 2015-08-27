package net.ravendb.abstractions.data;

import java.util.Date;

public class SideBySideIndexes {
    private IndexToAdd[] indexesToAdd;

    private Etag minimumEtagBeforeReplace;

    private Date replaceTimeUtc;

    public IndexToAdd[] getIndexesToAdd() {
        return indexesToAdd;
    }

    public void setIndexesToAdd(IndexToAdd[] indexesToAdd) {
        this.indexesToAdd = indexesToAdd;
    }

    public Etag getMinimumEtagBeforeReplace() {
        return minimumEtagBeforeReplace;
    }

    public void setMinimumEtagBeforeReplace(Etag minimumEtagBeforeReplace) {
        this.minimumEtagBeforeReplace = minimumEtagBeforeReplace;
    }

    public Date getReplaceTimeUtc() {
        return replaceTimeUtc;
    }

    public void setReplaceTimeUtc(Date replaceTimeUtc) {
        this.replaceTimeUtc = replaceTimeUtc;
    }
}
