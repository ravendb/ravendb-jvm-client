package net.ravendb.client.documents.operations.AI;

import net.ravendb.client.documents.operations.etl.AddEtlOperationResult;

public abstract class AddAiTaskOperationResult extends AddEtlOperationResult {

    private String identifier;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
