package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class PutCommandDataWithJson extends PutCommandDataBase<ObjectNode> {

    public PutCommandDataWithJson(String id, String changeVector, ObjectNode document) {
        super(id, changeVector, document);
    }

}
