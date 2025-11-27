package net.ravendb.client.documents.indexes;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Defines the priority levels that control the order of index processing.
 *
 * <p><b>Levels:</b></p>
 * <ul>
 *   <li><b>LOW</b> — Assigns a lower processing priority to the index.</li>
 *   <li><b>NORMAL</b> — Assigns a normal processing priority to the index.</li>
 *   <li><b>HIGH</b> — Assigns a higher processing priority to the index.</li>
 * </ul>
 */

@UseSharpEnum
public enum IndexPriority {
    LOW,
    NORMAL,
    HIGH
}
