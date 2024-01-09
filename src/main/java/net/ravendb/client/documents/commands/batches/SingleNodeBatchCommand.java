package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.ShardedBatchBehavior;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.BatchCommandResult;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.util.TimeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.InputStreamBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SingleNodeBatchCommand extends RavenCommand<BatchCommandResult> implements CleanCloseable {
    private Boolean _supportsAtomicWrites;
    private Set<InputStream> _attachmentStreams;
    private final DocumentConventions _conventions;
    private final List<ICommandData> _commands;
    private final BatchOptions _options;
    private final TransactionMode _mode;

    public SingleNodeBatchCommand(DocumentConventions conventions, List<ICommandData> commands) {
        this(conventions, commands, null, TransactionMode.SINGLE_NODE);
    }

    public SingleNodeBatchCommand(DocumentConventions conventions, List<ICommandData> commands, BatchOptions options) {
        this(conventions, commands, options, TransactionMode.SINGLE_NODE);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public SingleNodeBatchCommand(DocumentConventions conventions, List<ICommandData> commands, BatchOptions options, TransactionMode mode) {
        super(BatchCommandResult.class);
        this._commands = commands;
        this._options = options;
        this._conventions = conventions;
        this._mode = mode;

        if (conventions == null) {
            throw new IllegalArgumentException("conventions cannot be null");
        }

        if (commands == null) {
            throw new IllegalArgumentException("commands cannot be null");
        }

        for (int i = 0; i < commands.size(); i++) {
            ICommandData command = commands.get(i);

            if (command instanceof PutAttachmentCommandData) {
                PutAttachmentCommandData putAttachmentCommandData = (PutAttachmentCommandData) command;

                if (_attachmentStreams == null) {
                    _attachmentStreams = new LinkedHashSet<>();
                }

                InputStream stream = putAttachmentCommandData.getStream();
                if (!_attachmentStreams.add(stream)) {
                    PutAttachmentCommandHelper.throwStreamWasAlreadyUsed();
                }
            }
        }
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        HttpPost request = new HttpPost();

        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                if (_supportsAtomicWrites == null) {
                    _supportsAtomicWrites = node.isSupportsAtomicClusterWrites();
                }

                generator.writeStartObject();
                generator.writeFieldName("Commands");
                generator.writeStartArray();

                if (_supportsAtomicWrites) {
                    for (ICommandData command : _commands) {
                        command.serialize(generator, _conventions);
                    }
                } else {
                    for (ICommandData command : _commands) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try (JsonGenerator itemGenerator = createSafeJsonGenerator(baos)) {
                            command.serialize(itemGenerator, _conventions);
                        }

                        ObjectNode itemNode = (ObjectNode) mapper.readTree(baos.toByteArray());
                        itemNode.remove("OriginalChangeVector");
                        generator.writeObject(itemNode);
                    }
                }

                generator.writeEndArray();

                if (_mode == TransactionMode.CLUSTER_WIDE) {
                    generator.writeStringField("TransactionMode", "ClusterWide");
                }

                generator.writeEndObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ContentType.APPLICATION_JSON, _conventions));


        if (_attachmentStreams != null && _attachmentStreams.size() > 0) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();

            HttpEntity entity = request.getEntity();

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                entity.writeTo(baos);

                FormBodyPartBuilder mainPartBuilder = FormBodyPartBuilder
                        .create("main", new ByteArrayBody(baos.toByteArray(), "main"));

                if (entity.getContentEncoding() != null) {
                    mainPartBuilder.addField("Content-Encoding", entity.getContentEncoding().getValue());
                }

                entityBuilder.addPart(mainPartBuilder.build());
            } catch (IOException e) {
                throw new RavenException("Unable to serialize BatchCommand", e);
            }

            int nameCounter = 1;

            for (InputStream stream : _attachmentStreams) {
                InputStreamBody inputStreamBody = new InputStreamBody(stream, (String) null);
                FormBodyPart part = FormBodyPartBuilder.create("attachment" + nameCounter++, inputStreamBody)
                        .addField("Command-Type", "AttachmentStream")
                        .build();
                entityBuilder.addPart(part);
            }
            request.setEntity(entityBuilder.build());
        }

        StringBuilder sb = new StringBuilder(node.getUrl() + "/databases/" + node.getDatabase() + "/bulk_docs?");
        appendOptions(sb);

        url.value = sb.toString();
        return request;
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            throw new IllegalStateException("Got null response from the server after doing a batch, something is very wrong. Probably a garbled response.");
        }

        result = mapper.readValue(response, BatchCommandResult.class);
    }

    protected void appendOptions(StringBuilder sb) {
        if (_options == null) {
            return;
        }
        appendOptions(sb, _options.getIndexOptions(), _options.getReplicationOptions(), _options.getShardedOptions());

    }
    protected static void appendOptions(StringBuilder sb, IndexBatchOptions indexOptions, ReplicationBatchOptions replicationOptions, ShardedBatchOptions shardedOptions) {
        if (replicationOptions != null) {
            sb.append("&waitForReplicasTimeout=")
                    .append(TimeUtils.durationToTimeSpan(replicationOptions.getWaitForReplicasTimeout()));

            sb.append("&throwOnTimeoutInWaitForReplicas=")
                    .append(replicationOptions.isThrowOnTimeoutInWaitForReplicas() ? "true" : "false");

            sb.append("&numberOfReplicasToWaitFor=");
            sb.append(replicationOptions.isMajority() ? "majority" : replicationOptions.getNumberOfReplicasToWaitFor());
        }

        if (indexOptions != null) {
            sb.append("&waitForIndexesTimeout=")
                    .append(TimeUtils.durationToTimeSpan(indexOptions.getWaitForIndexesTimeout()));

            if (indexOptions.isThrowOnTimeoutInWaitForIndexes()) {
                sb.append("&waitForIndexThrow=true");
            } else {
                sb.append("&waitForIndexThrow=false");
            }

            if (indexOptions.getWaitForSpecificIndexes() != null) {
                for (String specificIndex : indexOptions.getWaitForSpecificIndexes()) {
                    sb.append("&waitForSpecificIndex=").append(urlEncode(specificIndex));
                }
            }
        }

        if (shardedOptions != null) {
            if (shardedOptions.getBatchBehavior() != ShardedBatchBehavior.DEFAULT) {
                sb.append("&shardedBatchBehavior=").append(SharpEnum.value(shardedOptions.getBatchBehavior()));
            }
        }
    }


    @Override
    public boolean isReadRequest() {
        return false;
    }

    @Override
    public void close() {
        // empty
    }
}
