package net.ravendb.client.documents.smuggler;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetNextOperationIdCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.http.*;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;

import java.io.*;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.ravendb.client.documents.smuggler.BackupUtils.BACKUP_FILE_SUFFIXES;

public class DatabaseSmuggler {

    private final IDocumentStore _store;
    private final String _databaseName;
    private final RequestExecutor _requestExecutor;

    public DatabaseSmuggler(IDocumentStore store) {
        this(store, null);
    }

    public DatabaseSmuggler(IDocumentStore store, String databaseName) {
        this._store = store;
        this._databaseName = ObjectUtils.firstNonNull(databaseName, store.getDatabase());
        if (_databaseName != null) {
            _requestExecutor = store.getRequestExecutor(_databaseName);
        } else {
            _requestExecutor = null;
        }
    }

    public DatabaseSmuggler forDatabase(String databaseName) {
        if (StringUtils.equalsIgnoreCase(databaseName, _databaseName)) {
            return this;
        }

        return new DatabaseSmuggler(_store, databaseName);
    }

    public Operation exportAsync(DatabaseSmugglerExportOptions options, String toFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(toFile)) {
            return exportAsync(options, response -> {
                try {
                    IOUtils.copyLarge(response, fos);
                } catch (IOException e) {
                    throw new RavenException("Unable to export database: " + e.getMessage(), e);
                }
            });
        }
    }

    private Operation exportAsync(DatabaseSmugglerExportOptions options, Consumer<InputStream> handleStreamResponse) {
        if (options == null) {
            throw new IllegalArgumentException("Options cannot be null");
        }

        if (_requestExecutor == null) {
            throw new IllegalStateException("Cannot use smuggler without a database defined, did you forget to call 'forDatabase'?");
        }

        GetNextOperationIdCommand getNextOperationIdCommand = new GetNextOperationIdCommand();
        _requestExecutor.execute(getNextOperationIdCommand);
        Long operationId = getNextOperationIdCommand.getResult();

        ExportCommand command = new ExportCommand(_requestExecutor.getConventions(), options, handleStreamResponse, operationId);
        _requestExecutor.execute(command);

        return new Operation(_requestExecutor, () -> _store.changes(_databaseName), _requestExecutor.getConventions(), operationId);
    }

    public Operation exportAsync(DatabaseSmugglerExportOptions options, DatabaseSmuggler toDatabase) {
        if (options == null) {
            throw new IllegalArgumentException("Options cannot be null");
        }

        if (toDatabase == null) {
            throw new IllegalArgumentException("ToDatabase cannot be null");
        }

        DatabaseSmugglerImportOptions importOptions = new DatabaseSmugglerImportOptions(options);
        Operation result = exportAsync(options, stream -> {
            toDatabase.importAsync(importOptions, stream);
        });

        return result;
    }

    public void importIncrementalAsync(DatabaseSmugglerImportOptions options, String fromDirectory) throws IOException {
        List<File> files = FileUtils.listFiles(new File(fromDirectory), new SuffixFileFilter(BACKUP_FILE_SUFFIXES, IOCase.INSENSITIVE), null)
                .stream()
                .sorted(BackupUtils.COMPARATOR)
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            return;
        }

        EnumSet<DatabaseItemType> oldOperateOnTypes = configureOptionsFromIncrementalImport(options);

        for (int i = 0; i < files.size() - 1; i++) {
            File filePath = files.get(i);
            importAsync(options, filePath.getAbsolutePath());
        }

        options.setOperateOnTypes(oldOperateOnTypes);

        File lastFile = files.get(files.size() - 1);
        importAsync(options, lastFile.getAbsolutePath());
    }

    public static EnumSet<DatabaseItemType> configureOptionsFromIncrementalImport(DatabaseSmugglerOptions options) {
        options.getOperateOnTypes().add(DatabaseItemType.TOMBSTONES);

        // we import the indexes and identities from the last file only,
        // as the previous files can hold indexes and identities which were deleted and shouldn't be imported
        EnumSet<DatabaseItemType> oldOperateOnTypes = options.getOperateOnTypes().clone();

        options.getOperateOnTypes().remove(DatabaseItemType.INDEXES);
        options.getOperateOnTypes().remove(DatabaseItemType.COMPARE_EXCHANGE);
        options.getOperateOnTypes().remove(DatabaseItemType.IDENTITIES);

        return oldOperateOnTypes;
    }

    public Operation importAsync(DatabaseSmugglerImportOptions options, String fromFile) throws IOException {
        int countOfFileParts = 0;

        Operation result;

        do {
            try (FileInputStream fos = new FileInputStream(fromFile)) {
                result = importAsync(options, fos);
            }
            countOfFileParts++;
            fromFile = String.format("%s.part%3d", fromFile, countOfFileParts);
        } while (new File(fromFile).exists());

        return result;
    }

    public Operation importAsync(DatabaseSmugglerImportOptions options, InputStream stream) {
        if (options == null) {
            throw new IllegalArgumentException("Options cannot be null");
        }

        if (stream == null) {
            throw new IllegalArgumentException("Stream cannot be null");
        }

        if (_requestExecutor == null) {
            throw new IllegalStateException("Cannot use smuggler without a database defined, did you forget to call 'forDatabase'?");
        }

        GetNextOperationIdCommand getNextOperationIdCommand = new GetNextOperationIdCommand();
        _requestExecutor.execute(getNextOperationIdCommand);
        Long operationId = getNextOperationIdCommand.getResult();

        ImportCommand command = new ImportCommand(_requestExecutor.getConventions(), options, stream, operationId);
        _requestExecutor.execute(command);

        return new Operation(_requestExecutor, () -> _store.changes(_databaseName), _requestExecutor.getConventions(), operationId);
    }

    private static class ExportCommand extends VoidRavenCommand {
        private final JsonNode _options;
        private final Consumer<InputStream> _handleStreamResponse;
        private final long _operationId;

        public ExportCommand(DocumentConventions conventions, DatabaseSmugglerExportOptions options,
                             Consumer<InputStream> handleStreamResponse, long operationId) {
            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }
            if (options == null) {
                throw new IllegalArgumentException("Options cannot be null");
            }
            if (handleStreamResponse == null) {
                throw new IllegalArgumentException("HandleStreamResponse cannot be null");
            }
            _handleStreamResponse = handleStreamResponse;
            _options = mapper.valueToTree(options);
            _operationId = operationId;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/smuggler/export?operationId=" + _operationId;

            HttpPost request = new HttpPost();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _options);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            return request;
        }

        @Override
        public ResponseDisposeHandling processResponse(HttpCache cache, CloseableHttpResponse response, String url) {
            try {
                _handleStreamResponse.accept(response.getEntity().getContent());
            } catch (IOException e) {
                throw new RavenException("Unable to export database: " + e.getMessage(), e);
            }

            return ResponseDisposeHandling.AUTOMATIC;
        }
    }

    private static class ImportCommand extends VoidRavenCommand {
        private final JsonNode _options;
        private final InputStream _stream;
        private final long _operationId;

        @Override
        public boolean isReadRequest() {
            return false;
        }

        public ImportCommand(DocumentConventions conventions, DatabaseSmugglerImportOptions options, InputStream stream, long operationId) {
            if (stream == null) {
                throw new IllegalArgumentException("Stream cannot be null");
            }
            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }
            if (options == null) {
                throw new IllegalArgumentException("Options cannot be null");
            }
            _stream = stream;
            _options = mapper.valueToTree(options);
            _operationId = operationId;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/smuggler/import?operationId=" + _operationId;

            try {
                MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();

                entityBuilder.addBinaryBody("importOptions", mapper.writeValueAsBytes(_options));
                InputStreamBody inputStreamBody = new InputStreamBody(_stream, "name");
                FormBodyPart part = FormBodyPartBuilder.create("file", inputStreamBody)
                        .build();
                entityBuilder.addPart(part);

                HttpPost request = new HttpPost();
                request.setEntity(entityBuilder.build());
                return request;
            } catch (JsonProcessingException e) {
                throw new RavenException("Unable to import database: " + e.getMessage(), e);
            }
        }
    }
}
