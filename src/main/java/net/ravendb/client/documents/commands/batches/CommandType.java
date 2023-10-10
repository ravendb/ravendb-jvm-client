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

    FORCE_REVISION_CREATION,

    COUNTERS,
    TIME_SERIES,
    TIME_SERIES_WITH_INCREMENTS,
    TIME_SERIES_BULK_INSERT,
    TIME_SERIES_COPY,

    BATCH_PATCH,

    JSON_PATCH,
    CLIENT_ANY_COMMAND,
    CLIENT_MODIFY_DOCUMENT_COMMAND,

    HEART_BEAT;

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
            case "ForceRevisionCreation":
                return FORCE_REVISION_CREATION;
            case "TimeSeries":
                return TIME_SERIES;
            case "TimeSeriesWithIncrements":
                return TIME_SERIES_WITH_INCREMENTS;
            default:
                throw new IllegalArgumentException("Unable to parse type: " + input);
        }
    }
}
