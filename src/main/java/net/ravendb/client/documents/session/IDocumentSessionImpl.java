package net.ravendb.client.documents.session;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.operations.lazy.IEagerSessionOperations;
import net.ravendb.client.documents.session.operations.lazy.ILazySessionOperations;

import java.util.Map;

public interface IDocumentSessionImpl extends IDocumentSession, ILazySessionOperations, IEagerSessionOperations {

    DocumentConventions getConventions();

    <T> Map<String, T> loadInternal(Class<T> clazz, String[] ids, String[] includes);

    //TBD: Lazy<Dictionary<string, T>> LazyLoadInternal<T>(string[] ids, string[] includes, Action<Dictionary<string, T>> onEval);
}
