package net.ravendb.client.documents.session.loaders;

import java.util.Date;

public interface IGenericRevisionIncludeBuilder<TBuilder> {
    TBuilder includeRevisions(String path);
    TBuilder includeRevisions(Date before);
}
