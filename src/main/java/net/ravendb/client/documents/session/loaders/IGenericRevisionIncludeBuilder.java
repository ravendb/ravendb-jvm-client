package net.ravendb.client.documents.session.loaders;

import java.util.Date;

/**
 * The server is instructed to include revisions of the specified documents when retrieving them.
 * Once loaded into the session, revisions can be accessed without additional server requests, ensuring efficient
 * retrieval of document history.
 */
public interface IGenericRevisionIncludeBuilder<TBuilder> {
    TBuilder includeRevisions(String path);
    /**
     * Include a single revision by specifying its creation time.
     * The specified time can be in local time or UTC; the server will convert it to UTC.
     * If an exact match for the creation time is found, that revision will be included.
     * Otherwise, the first revision preceding the specified time will be returned.
     *
     * @param before The creation time of the revision to include.
     */
    TBuilder includeRevisions(Date before);

    //TODO: add expression api
}
