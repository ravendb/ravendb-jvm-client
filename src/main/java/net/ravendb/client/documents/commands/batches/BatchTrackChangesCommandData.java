package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class BatchTrackChangesCommandData implements ICommandData {

    private final Map<String, String> trackedEntities;
    private final Set<String> _idsToSkip;

    public BatchTrackChangesCommandData(Map<String, String> trackedEntities, Set<String> idsToSkip) {
        this.trackedEntities = trackedEntities;
        this._idsToSkip = idsToSkip;
    }

    public Map<String, String> getTrackedEntities() {
        return trackedEntities;
    }

    @Override
    public String getId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getChangeVector() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CommandType getType() {
        return CommandType.BATCH_TRACK_CHANGES;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("Type", "BatchTrackChanges");

        generator.writeFieldName("TrackedEntities");
        generator.writeStartObject();

        for (Map.Entry<String, String> kvp : trackedEntities.entrySet()) {
            if (_idsToSkip.contains(kvp.getKey())) {
                continue;
            }

            generator.writeStringField(kvp.getKey(), kvp.getValue());
        }

        generator.writeEndObject();

        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
        // this command does not update session state after SaveChanges call!
    }
}
