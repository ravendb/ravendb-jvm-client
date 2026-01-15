package net.ravendb.client.documents.operations.AI;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class EmbeddingsTransformation {

    static final String GENERATE_EMBEDDINGS_FUNCTION_NAME = "embeddings.generate";
    private static final Pattern EMBEDDINGS_GENERATE_REGEX =
            Pattern.compile(GENERATE_EMBEDDINGS_FUNCTION_NAME);

    private String script;

    private ChunkingOptions chunkingOptions =
            new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256);

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public ChunkingOptions getChunkingOptions() {
        return chunkingOptions;
    }

    public void setChunkingOptions(ChunkingOptions chunkingOptions) {
        this.chunkingOptions = chunkingOptions;
    }

    void validate(List<String> errors) {
        validateScript(errors);
        chunkingOptions.validate(GENERATE_EMBEDDINGS_FUNCTION_NAME, errors);
    }

    private void validateScript(List<String> errors) {
        if (script == null) {
            errors.add("Transformation script must use " + GENERATE_EMBEDDINGS_FUNCTION_NAME + " method.");
            return;
        }

        Matcher match = EMBEDDINGS_GENERATE_REGEX.matcher(script);

        if (!match.find()) {
            errors.add("Transformation script must use " + GENERATE_EMBEDDINGS_FUNCTION_NAME + " method.");
        }
    }

    static boolean areEqual(EmbeddingsTransformation left, EmbeddingsTransformation right) {
        if (left == null && right == null)
            return true;

        if (left == null || right == null)
            return false;
        boolean sameScript =
                (left.script == null && right.script == null) ||
                        (left.script != null && left.script.equals(right.script));

        boolean sameChunking =
                ChunkingOptions.areEqual(left.chunkingOptions, right.chunkingOptions);

        return sameScript && sameChunking;
    }
}
