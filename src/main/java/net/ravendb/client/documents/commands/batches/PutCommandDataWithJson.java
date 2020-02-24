package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.session.ForceRevisionStrategy;

public class PutCommandDataWithJson extends PutCommandDataBase<ObjectNode> {

    public PutCommandDataWithJson(String id, String changeVector, ObjectNode document) {
        super(id, changeVector, document);
    }

    public PutCommandDataWithJson(String id, String changeVector, ObjectNode document, ForceRevisionStrategy strategy) {
        super(id, changeVector, document, strategy);
    }

}
