package net.ravendb.client.documents.session.operations.lazy;

/**
 * Specify interface for lazy operation for the session
 */
public interface ILazySessionOperations {

    //TBD ILazyLoaderWithInclude<object> Include(string path);
    //TBD ILazyLoaderWithInclude<TResult> Include<TResult>(Expression<Func<TResult, string>> path);
    //TBD ILazyLoaderWithInclude<TResult> Include<TResult>(Expression<Func<TResult, IEnumerable<string>>> path);
    //TBD Lazy<Dictionary<string, TResult>> Load<TResult>(IEnumerable<string> ids);
    //TBD Lazy<Dictionary<string, TResult>> Load<TResult>(IEnumerable<string> ids, Action<Dictionary<string, TResult>> onEval);
    //TBD Lazy<TResult> Load<TResult>(string id);
    //TBD Lazy<TResult> Load<TResult>(string id, Action<TResult> onEval);
    //TBD Lazy<Dictionary<string, TResult>> LoadStartingWith<TResult>(string idPrefix, string matches = null, int start = 0, int pageSize = 25, string exclude = null, string startAfter = null);
    //TBD Lazy<List<TResult>> MoreLikeThis<TResult>(MoreLikeThisQuery query);
}
