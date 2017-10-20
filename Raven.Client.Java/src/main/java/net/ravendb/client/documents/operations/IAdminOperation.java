package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;

public interface IAdminOperation<TResult> {
    RavenCommand<TResult> getCommand(DocumentConventions conventions, ObjectMapper context);
}
