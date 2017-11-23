package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Date;

public class GetConflictsResult {

    private String id;
    private Conflict[] results;
    private long largestEtag;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Conflict[] getResults() {
        return results;
    }

    public void setResults(Conflict[] results) {
        this.results = results;
    }

    public long getLargestEtag() {
        return largestEtag;
    }

    public void setLargestEtag(long largestEtag) {
        this.largestEtag = largestEtag;
    }

    public static class Conflict {
        private Date lastModified;
        private String changeVector;
        private ObjectNode doc;

        public Date getLastModified() {
            return lastModified;
        }

        public void setLastModified(Date lastModified) {
            this.lastModified = lastModified;
        }

        public String getChangeVector() {
            return changeVector;
        }

        public void setChangeVector(String changeVector) {
            this.changeVector = changeVector;
        }

        public ObjectNode getDoc() {
            return doc;
        }

        public void setDoc(ObjectNode doc) {
            this.doc = doc;
        }
    }

}
