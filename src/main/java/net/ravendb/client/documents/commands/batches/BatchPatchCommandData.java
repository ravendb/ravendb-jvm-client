package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.PatchRequest;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

/**
 * Commands that patches multiple documents using same patch script
 * CAUTION: This command does not update session state after .saveChanges() call
 */
public class BatchPatchCommandData implements ICommandData {

    public static class IdAndChangeVector {
        private String id;
        private String changeVector;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getChangeVector() {
            return changeVector;
        }

        public void setChangeVector(String changeVector) {
            this.changeVector = changeVector;
        }

        public static IdAndChangeVector create(String id, String changeVector) {
            IdAndChangeVector idAndChangeVector = new IdAndChangeVector();
            idAndChangeVector.setId(id);
            idAndChangeVector.setChangeVector(changeVector);
            return idAndChangeVector;
        }
    }

    private final Set<String> _seenIds = new TreeSet<>(String::compareToIgnoreCase);

    private final List<IdAndChangeVector> _ids = new ArrayList<>();

    private String name = null;

    private PatchRequest patch;

    private PatchRequest patchIfMissing;

    private BatchPatchCommandData(PatchRequest patch, PatchRequest patchIfMissing) {
        if (patch == null) {
            throw new IllegalArgumentException("Patch cannot be null");
        }

        this.patch = patch;
        this.patchIfMissing = patchIfMissing;
    }

    public BatchPatchCommandData(PatchRequest patch, PatchRequest patchIfMissing, String... ids) {
        this(patch, patchIfMissing);

        if (ids == null) {
            throw new IllegalArgumentException("Ids cannot be null");
        }

        if (ids.length == 0) {
            throw new IllegalArgumentException("Value cannot be an empty collection");
        }

        for (String id : ids) {
            add(id);
        }
    }

    public BatchPatchCommandData(PatchRequest patch, PatchRequest patchIfMissing, IdAndChangeVector... ids) {
        this(patch, patchIfMissing);

        if (ids == null) {
            throw new IllegalArgumentException("Ids cannot be null");
        }

        if (ids.length == 0) {
            throw new IllegalArgumentException("Value cannot be an empty collection");
        }

        for (IdAndChangeVector id : ids) {
            add(id.getId(), id.getChangeVector());
        }
    }

    private void add(String id) {
        add(id, null);
    }

    private void add(String id, String changeVector) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Value cannot be null or whitespace");
        }

        if (!_seenIds.add(id)) {
            throw new IllegalStateException("Could not add ID '" + id + "' because item with the same ID was already added");
        }

        _ids.add(IdAndChangeVector.create(id, changeVector));
    }

    public List<IdAndChangeVector> getIds() {
        return _ids;
    }

    @Override
    public String getId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return name;
    }

    public PatchRequest getPatch() {
        return patch;
    }

    public PatchRequest getPatchIfMissing() {
        return patchIfMissing;
    }

    @Override
    public String getChangeVector() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CommandType getType() {
        return CommandType.BATCH_PATCH;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();

        generator.writeFieldName("Ids");
        generator.writeStartArray();

        for (IdAndChangeVector kvp : _ids) {
            generator.writeStartObject();
            generator.writeStringField("Id", kvp.id);

            if (kvp.changeVector != null) {
                generator.writeStringField("ChangeVector", kvp.changeVector);
            }
            generator.writeEndObject();
        }

        generator.writeEndArray();

        generator.writeFieldName("Patch");
        patch.serialize(generator, conventions.getEntityMapper());

        generator.writeStringField("Type", "BatchPATCH");

        if (patchIfMissing != null) {
            generator.writeFieldName("PatchIfMissing");
            patchIfMissing.serialize(generator, conventions.getEntityMapper());
        }
        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
        // this command does not update session state after SaveChanges call!
    }
}
