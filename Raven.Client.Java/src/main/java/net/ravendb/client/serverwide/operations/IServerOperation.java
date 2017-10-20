package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;

public interface IServerOperation<T> {
    RavenCommand<T> getCommand(DocumentConventions conventions, ObjectMapper context);
}
