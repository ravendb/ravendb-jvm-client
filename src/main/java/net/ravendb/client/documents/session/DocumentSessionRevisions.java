package net.ravendb.client.documents.session;

import net.ravendb.client.documents.commands.GetRevisionsCommand;
import net.ravendb.client.documents.session.operations.GetRevisionOperation;
import net.ravendb.client.json.MetadataAsDictionary;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class DocumentSessionRevisions extends AdvancedSessionExtentionBase implements IRevisionsSessionOperations {

    public DocumentSessionRevisions(InMemoryDocumentSessionOperations session) {
        super(session);
    }

    @Override
    public <T> List<T> getFor(Class<T> clazz, String id) {
        return getFor(clazz, id, 0, 25);
    }

    @Override
    public <T> List<T> getFor(Class<T> clazz, String id, int start) {
        return getFor(clazz, id, start, 25);
    }

    @Override
    public <T> List<T> getFor(Class<T> clazz, String id, int start, int pageSize) {
        GetRevisionOperation operation = new GetRevisionOperation(session, id, start, pageSize);

        GetRevisionsCommand command = operation.createRequest();
        requestExecutor.execute(command, sessionInfo);
        operation.setResult(command.getResult());
        return operation.getRevisionsFor(clazz);
    }

    @Override
    public List<MetadataAsDictionary> getMetadataFor(String id) {
        return getMetadataFor(id, 0, 25);
    }

    @Override
    public List<MetadataAsDictionary> getMetadataFor(String id, int start) {
        return getMetadataFor(id, start, 25);
    }

    @Override
    public List<MetadataAsDictionary> getMetadataFor(String id, int start, int pageSize) {
        GetRevisionOperation operation = new GetRevisionOperation(session, id, start, pageSize, true);
        GetRevisionsCommand command = operation.createRequest();
        requestExecutor.execute(command, sessionInfo);
        operation.setResult(command.getResult());
        return operation.getRevisionsMetadataFor();
    }

    @Override
    public <T> T get(Class<T> clazz, String changeVector) {
        GetRevisionOperation operation = new GetRevisionOperation(session, changeVector);

        GetRevisionsCommand command = operation.createRequest();
        requestExecutor.execute(command, sessionInfo);
        operation.setResult(command.getResult());
        return operation.getRevision(clazz);
    }

    @Override
    public <T> Map<String, T> get(Class<T> clazz, String[] changeVectors) {
        GetRevisionOperation operation = new GetRevisionOperation(session, changeVectors);

        GetRevisionsCommand command = operation.createRequest();
        requestExecutor.execute(command, sessionInfo);
        operation.setResult(command.getResult());
        return operation.getRevisions(clazz);
    }

    @Override
    public <T> T get(Class<T> clazz, String id, Date date) {
        GetRevisionOperation operation = new GetRevisionOperation(session, id, date);
        GetRevisionsCommand command = operation.createRequest();
        requestExecutor.execute(command, sessionInfo);
        operation.setResult(command.getResult());
        return operation.getRevision(clazz);
    }
}
