package net.ravendb.client.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.session.DocumentInfo;
import net.ravendb.client.documents.session.DocumentsChanges;

import java.util.List;
import java.util.Map;

public class JsonOperation {

    public static boolean entityChanged(ObjectNode newObj, DocumentInfo documentInfo, Map<String, List<DocumentsChanges>> changes) {
        return false; //TODO:
    }
}
