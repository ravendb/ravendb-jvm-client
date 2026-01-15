package net.ravendb.client.documents.indexes;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Defines the modes for resetting an index.
 *
 * <p><b>Values:</b></p>
 * <ul>
 *   <li><b>IN_PLACE</b> — Resets the index in place, replacing the existing index with the newly rebuilt one.</li>
 *   <li><b>SIDE_BY_SIDE</b> — Resets the index in a side-by-side manner, allowing the new index to be built alongside the existing one.</li>
 * </ul>
 */
@UseSharpEnum
public enum IndexResetMode {
    IN_PLACE,
    SIDE_BY_SIDE
}
