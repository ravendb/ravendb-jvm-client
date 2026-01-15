package net.ravendb.client.documents.operations.etl;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Transformation {
    private String name;
    private boolean disabled;
    private List<String> collections = new ArrayList<>();
    private boolean applyToAllDocuments;
    private String script;
    boolean isLoadingAttachments;
    boolean isAddingAttachments;
    Map<String, String> collectionToDeleteDocumentsBehaviorFunction;
    boolean isEmptyScript;

    static final String LOAD_TO = "loadTo";
    static final String GENERIC_DELETE_DOCUMENTS_BEHAVIOR_FUNCTION_KEY = "$deleteDocumentsBehavior<>";
    static final String GENERIC_DELETE_DOCUMENTS_BEHAVIOR_FUNCTION_NAME = "deleteDocumentsBehavior";
    static final String ADD_ATTACHMENT = "addAttachment";
    static final String LOAD_ATTACHMENT = "loadAttachment";
    private static final String PARAMETERS_AND_FUNCTION_BODY_REGEX = "\\s*\\((?:[^)(]+|\\((?:[^)(]+|\\([^)(]*\\))*\\))*\\)\\s*\\{(?:[^}{]+|\\{(?:[^}{]+|\\{[^}{]*\\})*\\})*\\}";

    private static final Pattern LOAD_TO_METHOD_REGEX_ALT = Pattern.compile(LOAD_TO + "\\('([\\\\w\\\\.]*)'\\)|" + LOAD_TO + "\\(\"([\\\\w\\\\.]*)\"\\)");
    private static final Pattern LOAD_TO_METHOD_REGEX = Pattern.compile(LOAD_TO + "(\\w+)");
    static final Pattern DELETE_DOCUMENTS_BEHAVIOR_METHOD_REGEX = Pattern.compile("function\\s+deleteDocumentsOf(\\w+)Behavior" + PARAMETERS_AND_FUNCTION_BODY_REGEX, Pattern.DOTALL);
    static final Pattern DELETE_DOCUMENTS_BEHAVIOR_METHOD_NAME_REGEX = Pattern.compile("deleteDocumentsOf(\\w+)Behavior");
    private static final Pattern LOAD_ATTACHMENT_METHOD_REGEX = Pattern.compile(LOAD_ATTACHMENT);
    private static final Pattern LEGACY_REPLICATE_TO_METHOD_REGEX = Pattern.compile("replicateTo(\\w+)");
    private static final Pattern ADD_ATTACHMENT_METHOD_REGEX = Pattern.compile(ADD_ATTACHMENT);
    static final Pattern GENERIC_DELETE_DOCUMENTS_BEHAVIOR_METHOD_REGEX = Pattern.compile("function\\s+" + GENERIC_DELETE_DOCUMENTS_BEHAVIOR_FUNCTION_NAME + PARAMETERS_AND_FUNCTION_BODY_REGEX,Pattern.DOTALL);

    public Transformation() {
        this.counters = new CountersTransformation(this);
        this.timeSeries = new TimeSeriesTransformation(this);
    }

    public Map<String, String> getCollectionToDeleteDocumentsBehaviorFunction() {
        return collectionToDeleteDocumentsBehaviorFunction;
    }

    private void setCollectionToDeleteDocumentsBehaviorFunction(Map<String, String> value) {
        this.collectionToDeleteDocumentsBehaviorFunction = value;
    }

    public boolean isLoadingAttachments() {
        return isLoadingAttachments;
    }

    public boolean isEmptyScript() {
        return isEmptyScript;
    }

    public void setEmptyScript(boolean value) {
        this.isEmptyScript = value;
    }

    private void setLoadingAttachments(boolean value) {
        this.isLoadingAttachments = value;
    }

    public boolean isAddingAttachments() {
        return isAddingAttachments;
    }

    private void setAddingAttachments(boolean value) {
        this.isAddingAttachments = value;
    }

    private String documentIdPostfix;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public List<String> getCollections() {
        return collections;
    }

    public void setCollections(List<String> collections) {
        this.collections = collections;
    }

    public boolean isApplyToAllDocuments() {
        return applyToAllDocuments;
    }

    public void setApplyToAllDocuments(boolean applyToAllDocuments) {
        this.applyToAllDocuments = applyToAllDocuments;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getDocumentIdPostfix() {
        return documentIdPostfix;
    }

    public void setDocumentIdPostfix(String documentIdPostfix) {
        this.documentIdPostfix = documentIdPostfix;
    }

    public boolean validate(List<String> errors, EtlType type) {

        if (errors == null) {
            throw new IllegalArgumentException("errors cannot be null");
        }

        if (name == null || name.trim().isEmpty()) {
            errors.add("Script name cannot be empty");
        }

        if (applyToAllDocuments) {
            if (collections != null && !collections.isEmpty()) {
                errors.add("Collections cannot be specified when ApplyToAllDocuments is set. Script name: '" + name + "'");
            }
        } else {
            if (collections == null || collections.isEmpty()) {
                errors.add("Collections need be specified or ApplyToAllDocuments has to be set. Script name: '" + name + "'");
            }
        }

        if (script != null && !script.trim().isEmpty()) {

            if (LEGACY_REPLICATE_TO_METHOD_REGEX.matcher(script).find()) {
                errors.add("Found `replicateTo<TableName>()` method in '" + name + "' script which is not supported. "
                        + "If you are using the SQL replication script from RavenDB 3.x version then please use `loadTo<TableName>()` instead.");
            }

            isAddingAttachments = ADD_ATTACHMENT_METHOD_REGEX.matcher(script).find();
            isLoadingAttachments = LOAD_ATTACHMENT_METHOD_REGEX.matcher(script).find();

            counters.validate(errors, type);
            timeSeries.validate(errors, type);

            Matcher deleteBehaviors = DELETE_DOCUMENTS_BEHAVIOR_METHOD_REGEX.matcher(script);
            List<MatchResult> deleteBehaviorMatches = new ArrayList<>();
            while (deleteBehaviors.find()) {
                deleteBehaviorMatches.add(deleteBehaviors.toMatchResult());
            }

            if (!deleteBehaviorMatches.isEmpty()) {
                if (type == EtlType.SQL) {
                    errors.add("Delete documents behavior functions aren't supported by SQL ETL");
                } else {
                    collectionToDeleteDocumentsBehaviorFunction = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

                    for (MatchResult match : deleteBehaviorMatches) {

                        if (match.groupCount() != 1) {
                            errors.add("Invalid delete documents behavior function. It is expected to have the following signature: "
                                    + "deleteDocumentsOf<CollectionName>Behavior(docId) and return 'true' if document deletion should be sent to a destination");
                        }

                        String function = match.group(0);
                        String collection = match.group(1);

                        Matcher fn = DELETE_DOCUMENTS_BEHAVIOR_METHOD_NAME_REGEX.matcher(function);
                        String functionName = fn.find() ? fn.group() : null;

                        if (!collections.contains(collection)) {
                            String scriptCollections = collections.stream()
                                    .map(x -> "'" + x + "'")
                                    .collect(Collectors.joining(", "));

                            errors.add("There is '" + functionName + "' function defined in '" + name + "' script while the processed collections "
                                    + "(" + scriptCollections + ") doesn't include '" + collection + "'. "
                                    + "deleteDocumentsOf<CollectionName>Behavior() function is meant to be defined only for documents from collections that "
                                    + "are loaded to the same collection on a destination side");
                        }

                        collectionToDeleteDocumentsBehaviorFunction.put(collection, functionName);
                    }
                }
            }

            Matcher genericDeleteBehavior = GENERIC_DELETE_DOCUMENTS_BEHAVIOR_METHOD_REGEX.matcher(script);
            List<MatchResult> genericDeleteMatches = new ArrayList<>();
            while (genericDeleteBehavior.find()) {
                genericDeleteMatches.add(genericDeleteBehavior.toMatchResult());
            }

            if (!genericDeleteMatches.isEmpty()) {
                if (type == EtlType.SQL) {
                    errors.add("Delete documents behavior functions aren't supported by SQL ETL");
                } else {
                    if (genericDeleteMatches.size() > 1) {
                        errors.add("Generic delete behavior function can be defined just once in the script");
                    } else {
                        if (collectionToDeleteDocumentsBehaviorFunction == null) {
                            collectionToDeleteDocumentsBehaviorFunction = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                        }
                        collectionToDeleteDocumentsBehaviorFunction.put(
                                GENERIC_DELETE_DOCUMENTS_BEHAVIOR_FUNCTION_KEY,
                                GENERIC_DELETE_DOCUMENTS_BEHAVIOR_FUNCTION_NAME
                        );
                    }
                }
            }

            List<String> scriptCollections = getCollectionsFromScript();

            if (scriptCollections == null || scriptCollections.size() == 0) {

                String actualScript = script;

                for (MatchResult m : deleteBehaviorMatches) {
                    actualScript = actualScript.replace(m.group(), "");
                }

                if (genericDeleteMatches.size() == 1) {
                    actualScript = actualScript.replace(genericDeleteMatches.get(0).group(), "");
                }

                if (actualScript != null && !actualScript.trim().isEmpty()) {

                    String targetName;
                    switch (type) {
                        case RAVEN:
                            targetName = "Collection";
                            break;
                        case SQL:
                        case OLAP:
                            targetName = "Table";
                            break;
                        case ELASTIC_SEARCH:
                            targetName = "Index";
                            break;
                        case QUEUE:
                            targetName = "Queue";
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown ETL type: " + type);
                    }

                    errors.add("No `loadTo<" + targetName + "Name>()` method call found in '" + name + "' script");
                } else {
                    isEmptyScript = true;
                }
            }

        } else {
            isEmptyScript = true;
        }

        if (isEmptyScript) {
            if (type != EtlType.RAVEN &&
                    type != EtlType.EMBEDDINGS_GENERATION &&
                    type != EtlType.GEN_AI) {

                errors.add("Script '" + name + "' must not be empty");
            }
        }

        return errors.isEmpty();
    }

    final CountersTransformation counters;

    public CountersTransformation getCounters() { return counters; }

    final class CountersTransformation {

        static final String LOAD = "loadCounter";
        static final String ADD = "addCounter";
        static final String MARKER = "$counter/";

        private final Pattern ADD_METHOD_REGEX =
                Pattern.compile(ADD);

        private final Pattern LOAD_BEHAVIOR_METHOD_REGEX =
                Pattern.compile("function\\s+loadCountersOf(\\w+)Behavior\\s*\\(.+\\)");

        private final Pattern LOAD_BEHAVIOR_METHOD_NAME_REGEX =
                Pattern.compile("loadCountersOf(\\w+)Behavior");

        private final Transformation parent;

        // internal bool IsAddingCounters { get; private set; }
        boolean isAddingCounters;

        public boolean isAddingCounters() {
            return isAddingCounters;
        }

        private void setAddingCounters(boolean value) {
            this.isAddingCounters = value;
        }

        Map<String, String> collectionToLoadBehaviorFunction;

        public Map<String, String> getCollectionToLoadBehaviorFunction() {
            return collectionToLoadBehaviorFunction;
        }

        private void setCollectionToLoadBehaviorFunction(Map<String, String> value) {
            this.collectionToLoadBehaviorFunction = value;
        }

        public CountersTransformation(Transformation parent) {
            this.parent = parent;
        }

        void validate(List<String> errors, EtlType type) {

            setAddingCounters(
                    ADD_METHOD_REGEX.matcher(parent.getScript()).find()
            );

            if (isAddingCounters && type == EtlType.SQL) {
                errors.add("Adding counters isn't supported by SQL ETL");
            }

            fillCollectionToLoadCounterBehaviorFunction(errors, type);
        }

        private void fillCollectionToLoadCounterBehaviorFunction(List<String> errors, EtlType type) {

            Matcher matcher = LOAD_BEHAVIOR_METHOD_REGEX.matcher(parent.getScript());
            List<MatchResult> counterBehaviors = new ArrayList<>();

            while (matcher.find()) {
                counterBehaviors.add(matcher.toMatchResult());
            }

            if (counterBehaviors.isEmpty()) {
                return;
            }

            if (type == EtlType.SQL) {
                errors.add("Load counter behavior functions aren't supported by SQL ETL");
                return;
            }

            setCollectionToLoadBehaviorFunction(
                    new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
            );

            for (MatchResult match : counterBehaviors) {

                if (match.groupCount() != 1) {
                    errors.add(
                            "Invalid load counters behavior function. It is expected to have the following signature: " +
                                    "loadCountersOf<CollectionName>Behavior(docId, counterName) and return 'true' if counter should be loaded to a destination"
                    );
                }

                String functionSignature = match.group(0);
                String collection = match.group(1);

                Matcher fn = LOAD_BEHAVIOR_METHOD_NAME_REGEX.matcher(functionSignature);
                String functionName = fn.find() ? fn.group() : null;

                if (!parent.getCollections().contains(collection)) {

                    String scriptCollections = parent.getCollections().stream()
                            .map(x -> "'" + x + "'")
                            .collect(Collectors.joining(", "));

                    errors.add(
                            "There is '" + functionName + "' function defined in '" + parent.getName() +
                                    "' script while the processed collections (" + scriptCollections +
                                    ") doesn't include '" + collection + "'. " +
                                    "loadCountersOf<CollectionName>Behavior() function is meant to be defined only for counters of docs from collections that " +
                                    "are loaded to the same collection on a destination side"
                    );
                }
                else if (!Arrays.asList(parent.getCollectionsFromScript()).contains(collection)) {
                    errors.add("`" + functionName + "` function was defined while there is no load to " +
                            collection + ". Load behavior function applies only if load to default collection");
                }

                if (collectionToLoadBehaviorFunction.containsKey(collection)) {
                    errors.add("There are multiple '" + functionName + "' functions defined");
                }

                collectionToLoadBehaviorFunction.put(collection, functionName);
            }
        }
    }

    final TimeSeriesTransformation timeSeries;

    public TimeSeriesTransformation getTimeSeries() { return timeSeries; }

    static final class TimeSeriesTransformation {

        static final String MARKER = "$timeSeries/";

        static final class AddTimeSeries {
            static final String NAME = "addTimeSeries";
            public static final String SIGNATURE = "addTimeSeries(timeSeriesReference)";
            public static final int PARAMS_COUNT = 1;

            static final Pattern REGEX = Pattern.compile(NAME);
        }

        final class LoadTimeSeries {
            public static final String NAME = "loadTimeSeries";
            public static final String SIGNATURE = "loadTimeSeries(timeSeriesName, from, to)";
            public static final int MIN_PARAMS_COUNT = 1;
            public static final int MAX_PARAMS_COUNT = 3;
        }

        final class HasTimeSeries {
            public static final String NAME = "hasTimeSeries";
            public static final String SIGNATURE = "hasTimeSeries(timeSeriesName)";
            public static final int PARAMS_COUNT = 1;
        }

        final class GetTimeSeries {
            public static final String NAME = "getTimeSeries";
            public static final String SIGNATURE = "getTimeSeries()";
            public static final int PARAMS_COUNT = 0;
        }

        private static final class LoadTimeSeriesOfCollectionBehavior {
            public static final String SIGNATURE =
                    "loadTimeSeriesOf<CollectionName>Behavior(docId, timeSeriesName)";

            public static final int PARAMS_COUNT = 2;

            static final Pattern REGEX = Pattern.compile(
                    "function\\s+(loadTimeSeriesOf([A-Za-z]\\w*)Behavior)\\s*\\(\\s*"
                            + "(([a-zA-Z]\\w*)\\s*(?:,\\s*([a-zA-Z]\\w*)\\s*)*)?\\s*\\)"
            );
        }

        private final Transformation parent;

        boolean isAddingTimeSeries;
        Map<String, String> collectionToLoadBehaviorFunction;

        public boolean isAddingTimeSeries() {
            return isAddingTimeSeries;
        }

        private void setAddingTimeSeries(boolean value) {
            this.isAddingTimeSeries = value;
        }

        public Map<String, String> getCollectionToLoadBehaviorFunction() {
            return collectionToLoadBehaviorFunction;
        }

        private void setCollectionToLoadBehaviorFunction(Map<String, String> map) {
            this.collectionToLoadBehaviorFunction = map;
        }

        public TimeSeriesTransformation(Transformation parent) {
            this.parent = parent;
        }

        void validate(List<String> errors, EtlType type) {

            setAddingTimeSeries(
                    AddTimeSeries.REGEX.matcher(parent.getScript()).find()
            );

            if (isAddingTimeSeries && type == EtlType.SQL) {
                errors.add("Adding time series isn't supported by SQL ETL");
            }

            fillCollectionToLoadTimeSeriesBehaviorFunction(errors, type);
        }

        private void fillCollectionToLoadTimeSeriesBehaviorFunction(List<String> errors, EtlType type) {

            Matcher matcher = LoadTimeSeriesOfCollectionBehavior.REGEX.matcher(parent.getScript());
            List<MatchResult> matches = new ArrayList<>();

            while (matcher.find()) {
                matches.add(matcher.toMatchResult());
            }

            if (matches.isEmpty()) {
                return;
            }

            if (type == EtlType.SQL) {
                errors.add("Load time series behavior functions aren't supported by SQL ETL");
                return;
            }

            setCollectionToLoadBehaviorFunction(
                    new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
            );

            for (MatchResult m : matches) {

                String functionName = m.group(1);
                String collection = m.group(2);

                List<String> params = new ArrayList<>();
                if (m.group(4) != null) params.add(m.group(4));
                if (m.group(5) != null) params.add(m.group(5));

                if (params.size() > LoadTimeSeriesOfCollectionBehavior.PARAMS_COUNT) {
                    errors.add("'" + functionName + " function defined with " + params.size()
                            + ". The signature should be " + LoadTimeSeriesOfCollectionBehavior.SIGNATURE);
                }

                if (!parent.getCollections().contains(collection)) {

                    String scriptCollections = parent.getCollections().stream()
                            .map(x -> "'" + x + "'")
                            .collect(Collectors.joining(", "));

                    errors.add(
                            "There is '" + functionName + "' function defined in '" + parent.getName()
                                    + "' script while the processed collections (" + scriptCollections
                                    + ") doesn't include '" + collection + "'. "
                                    + LoadTimeSeriesOfCollectionBehavior.SIGNATURE
                                    + " function is meant to be defined only for time series of docs from collections that "
                                    + "are loaded to the same collection on a destination side"
                    );
                }
                else if (!Arrays.asList(parent.getCollectionsFromScript()).contains(collection)) {
                    errors.add("`" + functionName + "` function was defined while there is no load to "
                            + collection + ". Load behavior function applies only if load to default collection");
                }

                if (collectionToLoadBehaviorFunction.containsKey(collection)) {
                    errors.add("There are multiple '" + functionName + "' functions defined");
                }

                collectionToLoadBehaviorFunction.put(collection, functionName);
            }
        }
    }


    public List<String> getCollectionsFromScript() {

        if (collections != null) {
            return collections;
        }

        Matcher match = LOAD_TO_METHOD_REGEX.matcher(script);
        Matcher matchAlt = LOAD_TO_METHOD_REGEX_ALT.matcher(script);

        List<MatchResult> matches = new ArrayList<>();
        while (match.find()) {
            matches.add(match.toMatchResult());
        }

        List<MatchResult> matchesAlt = new ArrayList<>();
        while (matchAlt.find()) {
            matchesAlt.add(matchAlt.toMatchResult());
        }

        if (matches.isEmpty() && matchesAlt.isEmpty()) {
            return null;
        }

        collections = new ArrayList<>(matches.size() + matchesAlt.size());

        for (int i = 0; i < matches.size(); i++) {
            String full = matches.get(i).group(0);
            collections.add(full.substring(LOAD_TO.length()));
        }

        for (int i = 0; i < matchesAlt.size(); i++) {
            MatchResult m = matchesAlt.get(i);

            String g1 = null;
            String g2 = null;

            try { g1 = m.group(1); } catch (Exception ignored) {}
            try { g2 = m.group(2); } catch (Exception ignored) {}

            String collection = (g1 != null && !g1.isEmpty()) ? g1 : g2;

            collections.add(collection);
        }

        return collections;
    }
}
