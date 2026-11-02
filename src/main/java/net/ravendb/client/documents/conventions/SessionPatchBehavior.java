package net.ravendb.client.documents.conventions;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Controls the patch command format used by the session patch methods
 * (property set, array add/removeAt, map put/remove).
 */
@UseSharpEnum
public enum SessionPatchBehavior {
    /**
     * Generate RFC 6902 JsonPatch commands where possible (default). Operations without a
     * JsonPatch equivalent still fall back to JavaScript-based patch commands.
     */
    JSON_PATCH,

    /**
     * Always generate JavaScript-based patch commands (the behavior prior to JsonPatch support).
     * Use this to opt out of the JsonPatch code path entirely.
     */
    JAVA_SCRIPT
}
