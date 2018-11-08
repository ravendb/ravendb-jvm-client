package net.ravendb.client.documents.smuggler;

import java.util.ArrayList;
import java.util.List;

public class DatabaseSmugglerExportOptions extends DatabaseSmugglerOptions implements IDatabaseSmugglerExportOptions {

    private List<String> collections;

    public DatabaseSmugglerExportOptions() {
        collections = new ArrayList<>();
    }

    @Override
    public List<String> getCollections() {
        return collections;
    }

    @Override
    public void setCollections(List<String> collections) {
        this.collections = collections;
    }
}
