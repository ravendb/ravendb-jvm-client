package net.ravendb.client.documents.session;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.operations.lazy.IEagerSessionOperations;
import net.ravendb.client.documents.session.operations.lazy.ILazySessionOperations;

public interface IDocumentSessionImpl extends IDocumentSession, ILazySessionOperations, IEagerSessionOperations {

    DocumentConventions getConventions();


    /*  TODO:
        Dictionary<string, T> LoadInternal<T>(string[] ids, string[] includes);
        Lazy<Dictionary<string, T>> LazyLoadInternal<T>(string[] ids, string[] includes, Action<Dictionary<string, T>> onEval);
        */
}
