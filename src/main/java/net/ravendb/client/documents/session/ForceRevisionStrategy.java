package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum ForceRevisionStrategy {
    /**
     * Don't force revision
     */
    NONE,
    /**
     * Create a forced revision from the document that is currently in store - BEFORE applying any changes made by the user
     * The only exception is: with current implementation the forced revision for *new* document only is 'after', not 'before'
     */
    BEFORE
}
