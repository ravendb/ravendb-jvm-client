package net.ravendb.client.documents.commands.batches;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum CommandType {
    NONE,
    PUT,
    PATCH,
    DELETE,
    ATTACHMENT_PUT,
    ATTACHMENT_DELETE,
    ATTACHMENT_MOVE,
    ATTACHMENT_COPY,
    COMPARE_EXCHANGE_PUT,
    COMPARE_EXCHANGE_DELETE,

    COUNTERS,

    BATCH_PATCH,

    CLIENT_ANY_COMMAND,
    CLIENT_MODIFY_DOCUMENT_COMMAND;

    public static CommandType parseCSharpValue(String input) {
        switch (input) {
            case "None":
                return NONE;
            case "PUT":
                return PUT;
            case "PATCH":
                return PATCH;
            case "DELETE":
                return DELETE;
            case "AttachmentPUT":
                return ATTACHMENT_PUT;
            case "AttachmentDELETE":
                return ATTACHMENT_DELETE;
            case "AttachmentMOVE":
                return ATTACHMENT_MOVE;
            case "AttachmentCOPY":
                return ATTACHMENT_COPY;
            case "CompareExchangePUT":
                return COMPARE_EXCHANGE_PUT;
            case "CompareExchangeDELETE":
                return COMPARE_EXCHANGE_DELETE;
            case "Counters":
                return COUNTERS;
            case "BatchPATCH":
                return BATCH_PATCH;
            default:
                throw new IllegalArgumentException("Unable to parse type: " + input);
        }
    }
}
