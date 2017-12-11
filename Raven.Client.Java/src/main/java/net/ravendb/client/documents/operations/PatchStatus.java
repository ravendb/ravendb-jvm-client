package net.ravendb.client.documents.operations;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * The result of a patch operation
 */
@UseSharpEnum
public enum PatchStatus {

    /**
     * The document does not exists, operation was a no-op
     */
    DOCUMENT_DOES_NOT_EXIST,

    /**
     * The document did not exist, but patchIfMissing was specified and new document was created
     */
    CREATED,

    /**
     * The document was properly patched
     */
    PATCHED,

    /**
     * The document was not patched, because skipPatchIfChangeVectorMismatch was set and the etag did not match
     */
    SKIPPED,

    /**
     * Neither document body not metadata was changed during patch operation
     */
    NOT_MODIFIED
}
