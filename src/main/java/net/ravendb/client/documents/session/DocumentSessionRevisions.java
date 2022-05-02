package net.ravendb.client.documents.session;

import net.ravendb.client.documents.commands.GetRevisionsCommand;
import net.ravendb.client.documents.session.operations.GetRevisionOperation;
import net.ravendb.client.documents.session.operations.GetRevisionsCountOperation;
import net.ravendb.client.documents.session.operations.lazy.LazyRevisionOperations;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.json.MetadataAsDictionary;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class DocumentSessionRevisions extends DocumentSessionRevisionsBase implements IRevisionsSessionOperations {

    public DocumentSessionRevisions(InMemoryDocumentSessionOperations session) {
        super(session);
    }

    @Override
    public ILazyRevisionsOperations lazily() {
        return new LazyRevisionOperations((DocumentSession) session);
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
        if (command == null) {
            return operation.getRevisionsFor(clazz);
        }
        if (sessionInfo != null) {
            sessionInfo.incrementRequestCount();
        }
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
        if (command == null) {
            return operation.getRevisionsMetadataFor();
        }
        if (sessionInfo != null) {
            sessionInfo.incrementRequestCount();
        }
        requestExecutor.execute(command, sessionInfo);
        operation.setResult(command.getResult());
        return operation.getRevisionsMetadataFor();
    }

    @Override
    public <T> T get(Class<T> clazz, String changeVector) {
        GetRevisionOperation operation = new GetRevisionOperation(session, changeVector);

        GetRevisionsCommand command = operation.createRequest();
        if (command == null) {
            return operation.getRevision(clazz);
        }
        if (sessionInfo != null) {
            sessionInfo.incrementRequestCount();
        }
        requestExecutor.execute(command, sessionInfo);
        operation.setResult(command.getResult());
        return operation.getRevision(clazz);
    }

    @Override
    public <T> Map<String, T> get(Class<T> clazz, String[] changeVectors) {
        GetRevisionOperation operation = new GetRevisionOperation(session, changeVectors);

        GetRevisionsCommand command = operation.createRequest();
        if (command == null) {
            return operation.getRevisions(clazz);
        }
        if (sessionInfo != null) {
            sessionInfo.incrementRequestCount();
        }
        requestExecutor.execute(command, sessionInfo);
        operation.setResult(command.getResult());
        return operation.getRevisions(clazz);
    }

    @Override
    public <T> T get(Class<T> clazz, String id, Date date) {
        GetRevisionOperation operation = new GetRevisionOperation(session, id, date);
        GetRevisionsCommand command = operation.createRequest();
        if (command == null) {
            return operation.getRevision(clazz);
        }
        if (sessionInfo != null) {
            sessionInfo.incrementRequestCount();
        }
        requestExecutor.execute(command, sessionInfo);
        operation.setResult(command.getResult());
        return operation.getRevision(clazz);
    }

    @Override
    public long getCountFor(String id) {
        GetRevisionsCountOperation operation = new GetRevisionsCountOperation(id);
        RavenCommand<Long> command = operation.createRequest();
        if (sessionInfo != null) {
            sessionInfo.incrementRequestCount();
        }
        requestExecutor.execute(command, sessionInfo);
        return command.getResult();
    }
}