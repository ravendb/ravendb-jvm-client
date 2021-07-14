package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.session.ForceRevisionStrategy;

public class PutCommandDataWithJson extends PutCommandDataBase<ObjectNode> {

    public PutCommandDataWithJson(String id, String changeVector, String originalChangeVector, ObjectNode document) {
        super(id, changeVector, originalChangeVector, document);
    }

    public PutCommandDataWithJson(String id, String changeVector, String originalChangeVector, ObjectNode document, ForceRevisionStrategy strategy) {
        super(id, changeVector, originalChangeVector, document, strategy);
    }

}
