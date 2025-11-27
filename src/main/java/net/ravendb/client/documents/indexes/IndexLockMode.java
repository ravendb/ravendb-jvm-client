package net.ravendb.client.documents.indexes;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Defines the lock modes that control the behavior of index modifications.
 *
 * <p><b>Modes:</b></p>
 * <ul>
 *   <li><b>UNLOCK</b> — Allows all index modifications.</li>
 *   <li><b>LOCKED_IGNORE</b> — Ignores all modification attempts without raising errors.</li>
 *   <li><b>LOCKED_ERROR</b> — Blocks all modifications and raises errors if modification is attempted.</li>
 * </ul>
 */
@UseSharpEnum
public enum IndexLockMode {
    UNLOCK,
    LOCKED_IGNORE,
    LOCKED_ERROR
}
