package net.ravendb.client.documents.commands.batches;

public enum CommandType {
    NONE,
    PUT,
    PATCH,
    DELETE,
    ATTACHMENT_PUT,
    ATTACHMENT_DELETE,

    CLIENT_ANY_COMMAND,
    CLIENT_NOT_ATTACHMENT
}
