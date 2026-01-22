package net.ravendb.client.documents.operations.AI;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum ChunkingMethod {
    PLAIN_TEXT_SPLIT,
    PLAIN_TEXT_SPLIT_LINES,
    PLAIN_TEXT_SPLIT_PARAGRAPHS,
    MARK_DOWN_SPLIT_LINES,
    MARK_DOWN_SPLIT_PARAGRAPHS,
    HTML_STRIP
}