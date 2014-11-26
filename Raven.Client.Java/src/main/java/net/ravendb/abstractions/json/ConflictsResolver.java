package net.ravendb.abstractions.json;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import net.ravendb.abstractions.json.linq.JTokenType;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.json.linq.RavenJTokenComparator;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;


public class ConflictsResolver {

  private RavenJObject[] docs;
  private RavenJTokenComparator ravenJTokenComparator = new RavenJTokenComparator();

  private final boolean isMetadataResolver;

  public ConflictsResolver(RavenJObject... result) {
    this(result, false);
  }

  public ConflictsResolver(RavenJObject[] docs, boolean isMetadataResolver) {
    this.docs = docs;
    this.isMetadataResolver = isMetadataResolver;
  }

  public MergeResult resolve() throws JsonGenerationException, IOException {
    return resolve(1);
  }

  public MergeResult resolve(int indent) throws JsonGenerationException, IOException {
    Map<String, Object> result = new HashMap<>();
    for (int index = 0; index < docs.length; index++) {
      RavenJObject doc = docs[index];
      for (Entry<String, RavenJToken> prop : doc) {
        if (result.containsKey(prop.getKey())) { // already dealt with
          continue;
        }

        JTokenType type = prop.getValue().getType();
        boolean executeDefault = true;
        if (type == JTokenType.OBJECT && tryHandleObjectValue(index, result, prop)) {
          executeDefault = false;
        }
        if (type == JTokenType.ARRAY && tryHandleArrayValue(index, result, prop)) {
          executeDefault = false;
        }
        if (executeDefault) {
          handleSimpleValues(result, prop, index);
        }
      }
    }
    return generateOutput(result, indent);
  }

  private boolean tryHandleArrayValue(int index, Map<String, Object> result, Map.Entry<String, RavenJToken> prop) {
    List<RavenJArray> arrays = new ArrayList<>();
    arrays.add((RavenJArray) prop.getValue());

    for (int i = 0; i < docs.length; i++) {
      if (i == index) {
        continue;
      }

      RavenJToken token = null;
      if (docs[i].containsKey(prop.getKey())) {
        token = docs[i].get(prop.getKey());
        if (token.getType() != JTokenType.ARRAY) {
          return false;
        }
        if (token.isSnapshot()) {
          token = token.createSnapshot();
        }
        arrays.add((RavenJArray) token);
      }
    }

    RavenJArray mergedArray = new RavenJArray();
    while (arrays.size() > 0) {
      Set<RavenJToken> set = new TreeSet<>(ravenJTokenComparator);
      for (int i = 0; i < arrays.size(); i++) {
        if (arrays.get(i).size() == 0) {
          arrays.remove(i);
          i--;
          continue;
        }
        set.add(arrays.get(i).get(0));
        arrays.get(i).removeAt(0);
      }

      for (RavenJToken ravenJToken : set) {
        mergedArray.add(ravenJToken);
      }
    }

    if (ravenJTokenComparator.compare(mergedArray, prop.getValue()) == 0) {
      result.put(prop.getKey(), mergedArray);
      return true;
    }

    result.put(prop.getKey(), new ArrayWithWarning(mergedArray));
    return true;

  }

  private boolean tryHandleObjectValue(int index, Map<String, Object> result, Map.Entry<String, RavenJToken> prop) {
    List<RavenJObject> others = new ArrayList<>();
    others.add((RavenJObject) prop.getValue());

    for (int i = 0; i < docs.length; i++) {
      if (i == index) {
        continue;
      }

      RavenJToken token = null;
      if (docs[i].containsKey(prop.getKey())) {
        token = docs[i].get(prop.getKey());
        if (token.getType() != JTokenType.OBJECT) {
          return false;
        }
        others.add((RavenJObject)token);
      }
    }
    result.put(prop.getKey(), new ConflictsResolver(others.toArray(new RavenJObject[0]), prop.getKey().equals("@metadata") || isMetadataResolver));
    return true;
  }


  private void handleSimpleValues(Map<String, Object> result, Map.Entry<String, RavenJToken> prop, int index) {
    Conflicted conflicted = new Conflicted();
    conflicted.getValues().add(prop.getValue());

    for (int i = 0; i < docs.length; i++) {
      if (i == index) {
        continue;
      }
      RavenJObject other = docs[i];
      RavenJToken otherVal = null;
      if (!other.containsKey(prop.getKey())) {
        continue;
      }
      otherVal = other.get(prop.getKey());

      if (ravenJTokenComparator.compare(prop.getValue(), otherVal) != 0) {
        conflicted.getValues().add(otherVal);
      }
    }
    if (conflicted.getValues().size() == 1) {
      result.put(prop.getKey(), prop.getValue());
    } else {
      result.put(prop.getKey(), conflicted);
    }
  }

  private void writeToken(JsonGenerator writer, String propertyName, Object propertyValue) throws JsonGenerationException, IOException {
    if (isMetadataResolver &&
      (propertyName.startsWith("Raven-Replication-")
        || propertyName.startsWith("@")
        || "Last-Modified".equals(propertyName)
        || "Raven-Last-Modified".equals(propertyName))) {
      return;
    }

    writer.writeFieldName(propertyName);
    if (propertyValue instanceof RavenJToken) {
      RavenJToken ravenJToken = (RavenJToken) propertyValue;
      ravenJToken.writeTo(writer);
      return;
    }
    if (propertyValue instanceof Conflicted) {
      Conflicted conflicted = (Conflicted) propertyValue;
      writer.writeRaw("/* >>>> conflict start */");
      writer.writeStartArray();
      for (RavenJToken token: conflicted.getValues()) {
        token.writeTo(writer);
      }
      writer.writeEndArray();
      writer.writeRaw("/* <<<< conflict end */");
      return;
    }

    if (propertyValue instanceof ArrayWithWarning) {
      ArrayWithWarning arrayWithWarning = (ArrayWithWarning) propertyValue;
      writer.writeRaw("/* >>>> auto merged array start */");
      arrayWithWarning.getMergedArray().writeTo(writer);
      writer.writeRaw("/* <<<< auto merged array end */");
      return;
    }

    throw new IllegalStateException("Could not understand how to deal with: " + propertyValue);
  }

  private void writeRawData(JsonGenerator writer, String data, int indent) throws JsonGenerationException, IOException {
    StringBuffer sb = new StringBuffer();
    String[] lines = data.split("\n");
    boolean first = true;
    for (String line: lines) {
      if (!first) {
        sb.append('\n');
        sb.append(StringUtils.repeat(" ", 2 * indent));
      }

      sb.append(line);
      first = false;
    }

    writer.writeRawValue(sb.toString());
  }

  private void writeConflictResolver(String name, JsonGenerator documentWriter, JsonGenerator metadataWriter, ConflictsResolver resolver, int indent) throws JsonGenerationException, IOException {
    MergeResult result = resolver.resolve(indent);

    if (resolver.isMetadataResolver) {
      if (!name.equals("@metadata")) {
        metadataWriter.writeFieldName(name);
      }
      writeRawData(metadataWriter, result.getDocument(), indent);
    } else {
      documentWriter.writeFieldName(name);
      writeRawData(documentWriter, result.getDocument(), indent);
    }
  }

  private MergeResult generateOutput(Map<String, Object> result, int indent) throws JsonGenerationException, IOException {

    JsonFactory factory = new JsonFactory();
    StringWriter documentStringWriter = new StringWriter();
    JsonGenerator documentWriter = factory.createJsonGenerator(documentStringWriter);

    StringWriter metadataStringWriter = new StringWriter();
    JsonGenerator metadataWriter = factory.createJsonGenerator(metadataStringWriter);

    documentWriter.writeStartObject();
    for (Map.Entry<String, Object> o: result.entrySet()) {
      if (o.getValue() instanceof ConflictsResolver) {
        writeConflictResolver(o.getKey(), documentWriter, metadataWriter, (ConflictsResolver) o.getValue(), "@metadata".equals(o.getKey()) ? 0 : indent + 1);
      } else {
        writeToken("@metadata".equals(o.getKey()) ? metadataWriter : documentWriter, o.getKey(), o.getValue());
      }
    }
    documentWriter.writeEndObject();

    documentWriter.close();
    metadataWriter.close();

    MergeResult mergeResult = new MergeResult();
    mergeResult.setDocument(documentStringWriter.getBuffer().toString());
    mergeResult.setMetadata(metadataStringWriter.getBuffer().toString());
    return mergeResult;

  }


  public static class Conflicted {
    private final Set<RavenJToken> values = new TreeSet<>(new RavenJTokenComparator());

    public Set<RavenJToken> getValues() {
      return values;
    }


  }

  public static class ArrayWithWarning {
    private final RavenJArray mergedArray;

    public RavenJArray getMergedArray() {
      return mergedArray;
    }

    public ArrayWithWarning(RavenJArray mergedArray) {
      super();
      this.mergedArray = mergedArray;
    }
  }

  public static class MergeChunk {
    private boolean isMetadata;
    private String data;

    public boolean isMetadata() {
      return isMetadata;
    }

    public void setMetadata(boolean isMetadata) {
      this.isMetadata = isMetadata;
    }

    public String getData() {
      return data;
    }

    public void setData(String data) {
      this.data = data;
    }

  }

  public static class MergeResult {
    private String document;
    private String metadata;

    public String getDocument() {
      return document;
    }

    public void setDocument(String document) {
      this.document = document;
    }

    public String getMetadata() {
      return metadata;
    }

    public void setMetadata(String metadata) {
      this.metadata = metadata;
    }

  }

}
