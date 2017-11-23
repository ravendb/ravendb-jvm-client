package net.ravendb.client.documents.indexes;

public class IndexingStatus {

    private IndexRunningStatus status;

    private IndexStatus[] indexes;

    public IndexRunningStatus getStatus() {
        return status;
    }

    public void setStatus(IndexRunningStatus status) {
        this.status = status;
    }

    public IndexStatus[] getIndexes() {
        return indexes;
    }

    public void setIndexes(IndexStatus[] indexes) {
        this.indexes = indexes;
    }

    public static class IndexStatus {
        private String name;
        private IndexRunningStatus status;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public IndexRunningStatus getStatus() {
            return status;
        }

        public void setStatus(IndexRunningStatus status) {
            this.status = status;
        }
    }
}
