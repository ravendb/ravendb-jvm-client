package net.ravendb.client.documents.smuggler;

import java.util.List;

public interface IDatabaseSmugglerExportOptions extends IDatabaseSmugglerOptions {

    List<String> getCollections();

    void setCollections(List<String> collections);

}
