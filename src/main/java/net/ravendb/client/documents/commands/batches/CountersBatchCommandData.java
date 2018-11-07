package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Lists;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.counters.CounterOperation;
import net.ravendb.client.documents.operations.counters.CounterOperationType;
import net.ravendb.client.documents.operations.counters.DocumentCountersOperation;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

public class CountersBatchCommandData implements ICommandData {

    private String id;
    private String name;
    private String changeVector;

    private Boolean fromEtl;
    private DocumentCountersOperation counters;

    public CountersBatchCommandData(String documentId, CounterOperation counterOperation) {
        this(documentId, Lists.newArrayList(counterOperation));

        if (counterOperation == null) {
            throw new IllegalArgumentException("CounterOperation cannot be null");
        }
    }

    public CountersBatchCommandData(String documentId, List<CounterOperation> counterOperations) {
        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be null");
        }

        this.id = documentId;
        this.name = null;
        this.changeVector = null;

        counters = new DocumentCountersOperation();
        counters.setDocumentId(documentId);
        counters.setOperations(counterOperations);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getChangeVector() {
        return changeVector;
    }

    public Boolean getFromEtl() {
        return fromEtl;
    }

    public DocumentCountersOperation getCounters() {
        return counters;
    }

    @Override
    public CommandType getType() {
        return CommandType.COUNTERS;
    }

    public boolean hasDelete(String counterName) {
        return hasOperationType(CounterOperationType.DELETE, counterName);
    }

    public boolean hasIncrement(String counterName) {
        return hasOperationType(CounterOperationType.INCREMENT, counterName);
    }

    private boolean hasOperationType(CounterOperationType type, String counterName) {
        for (CounterOperation op : counters.getOperations()) {
            if (!counterName.equals(op.getCounterName())) {
                continue;
            }

            if (op.getType() == type) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("Id", id);
        generator.writeFieldName("Counters");
        counters.serialize(generator, conventions);
        generator.writeObjectField("Type", "Counters");

        if (fromEtl != null) {
            generator.writeBooleanField("FromEtl", fromEtl);
        }
        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
    }
}
