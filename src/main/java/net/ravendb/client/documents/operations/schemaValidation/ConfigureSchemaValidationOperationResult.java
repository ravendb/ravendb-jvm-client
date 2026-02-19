package net.ravendb.client.documents.operations.schemaValidation;

public class ConfigureSchemaValidationOperationResult {

    private Long raftCommandIndex;

    public Long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(Long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
