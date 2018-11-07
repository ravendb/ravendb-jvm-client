package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class PatchOperation implements IOperation<PatchResult> {

    public static class Payload {
        private final PatchRequest patch;
        private final PatchRequest patchIfMissing;

        public Payload(PatchRequest patch, PatchRequest patchIfMissing) {
            this.patch = patch;
            this.patchIfMissing = patchIfMissing;
        }

        public PatchRequest getPatch() {
            return patch;
        }

        public PatchRequest getPatchIfMissing() {
            return patchIfMissing;
        }
    }

    public static class Result<TEntity> {
        private PatchStatus status;
        private TEntity document;

        public PatchStatus getStatus() {
            return status;
        }

        public void setStatus(PatchStatus status) {
            this.status = status;
        }

        public TEntity getDocument() {
            return document;
        }

        public void setDocument(TEntity document) {
            this.document = document;
        }
    }

    private final String _id;
    private final String _changeVector;
    private final PatchRequest _patch;
    private final PatchRequest _patchIfMissing;
    private final boolean _skipPatchIfChangeVectorMismatch;

    public PatchOperation(String id, String changeVector, PatchRequest patch) {
        this(id, changeVector, patch, null, false);
    }

    public PatchOperation(String id, String changeVector, PatchRequest patch, PatchRequest patchIfMissing, boolean skipPatchIfChangeVectorMismatch) {
        if (patch == null) {
            throw new IllegalArgumentException("Patch cannot be null");
        }

        if (StringUtils.isBlank(patch.getScript())) {
            throw new IllegalArgumentException("Patch script cannot be null");
        }

        if (patchIfMissing != null && StringUtils.isBlank(patchIfMissing.getScript())) {
            throw new IllegalArgumentException("PatchIfMissing script cannot be null");
        }

        _id = id;
        _changeVector = changeVector;
        _patch = patch;
        _patchIfMissing = patchIfMissing;
        _skipPatchIfChangeVectorMismatch = skipPatchIfChangeVectorMismatch;

    }

    @Override
    public RavenCommand<PatchResult> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new PatchCommand(conventions, _id, _changeVector, _patch, _patchIfMissing, _skipPatchIfChangeVectorMismatch, false, false);
    }

    public RavenCommand<PatchResult> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache, boolean returnDebugInformation, boolean test) {
        return new PatchCommand(conventions, _id, _changeVector, _patch, _patchIfMissing, _skipPatchIfChangeVectorMismatch, returnDebugInformation, test);
    }

    public static class PatchCommand extends RavenCommand<PatchResult> {
        private final DocumentConventions _conventions;
        private final String _id;
        private final String _changeVector;
        private final Payload _patch;
        private final boolean _skipPatchIfChangeVectorMismatch;
        private final boolean _returnDebugInformation;
        private final boolean _test;

        public PatchCommand(DocumentConventions conventions, String id, String changeVector,
                            PatchRequest patch, PatchRequest patchIfMissing, boolean skipPatchIfChangeVectorMismatch,
                            boolean returnDebugInformation, boolean test) {
            super(PatchResult.class);

            _conventions = conventions;

            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }

            if (patch == null) {
                throw new IllegalArgumentException("Patch cannot be null");
            }

            if (StringUtils.isBlank(patch.getScript())) {
                throw new IllegalArgumentException("Patch.Script cannot be null");
            }

            if (patchIfMissing != null && StringUtils.isBlank(patchIfMissing.getScript())) {
                throw new IllegalArgumentException("PatchIfMissing.Script cannot be null");
            }

            if (id == null) {
                throw new IllegalArgumentException("Id cannot be null");
            }

            _id = id;
            _changeVector = changeVector;
            _patch = new Payload(patch, patchIfMissing);
            _skipPatchIfChangeVectorMismatch = skipPatchIfChangeVectorMismatch;
            _returnDebugInformation = returnDebugInformation;
            _test = test;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/docs?id=" + UrlUtils.escapeDataString(_id);

            if (_skipPatchIfChangeVectorMismatch) {
                url.value += "&skipPatchIfChangeVectorMismatch=true";
            }

            if (_returnDebugInformation) {
                url.value += "&debug=true";
            }

            if (_test) {
                url.value += "&test=true";
            }

            HttpPatch request = new HttpPatch();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.writeStartObject();

                    generator.writeFieldName("Patch");
                    if (_patch.getPatch() != null) {
                        _patch.getPatch().serialize(generator, _conventions.getEntityMapper());
                    } else {
                        generator.writeNull();
                    }

                    generator.writeFieldName("PatchIfMissing");

                    if (_patch.getPatchIfMissing() != null) {
                        _patch.getPatchIfMissing().serialize(generator, _conventions.getEntityMapper());
                    } else {
                        generator.writeNull();
                    }

                    generator.writeEndObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));
            addChangeVectorIfNotNull(_changeVector, request);
            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            result = mapper.readValue(response, resultClass);
        }
    }
}
