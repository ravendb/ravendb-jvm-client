package net.ravendb.client.documents.smuggler;

public interface IDatabaseSmugglerImportOptions extends IDatabaseSmugglerOptions {

    boolean isSkipRevisionCreation();

    void setSkipRevisionCreation(boolean skipRevisionCreation);
}
