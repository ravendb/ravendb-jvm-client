package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.conventions.DocumentConventions;

import java.util.*;

public class IncludeBuilderBase {

    protected int nextParameterId = 1;

    protected final DocumentConventions _conventions;
    public Set<String> documentsToInclude;

    public String alias;




    public IncludeBuilderBase(DocumentConventions conventions) {
        _conventions = conventions;
    }




    protected void _includeDocuments(String path) {
        if (documentsToInclude == null) {
            documentsToInclude = new HashSet<>();
        }

        documentsToInclude.add(path);
    }

    protected void _withAlias() {
        if (alias == null) {
            alias = "a_" + (nextParameterId++);
        }
    }


}
