package net.ravendb.client.documents.commands.batches;

import net.ravendb.client.primitives.UseSharpEnum;

public enum CommandType {
    NONE,
    PUT,
    PATCH,
    DELETE,
    ATTACHMENT_PUT,
    ATTACHMENT_DELETE,

    CLIENT_ANY_COMMAND,
    CLIENT_NOT_ATTACHMENT_PUT
}
