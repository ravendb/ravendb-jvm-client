package net.ravendb.abstractions.data;

import java.util.ArrayList;
import java.util.List;

import net.ravendb.abstractions.basic.SharpEnum;
import net.ravendb.abstractions.json.linq.JTokenType;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.json.linq.RavenJValue;


/**
 *  A patch request for a specified document
 */
public class PatchRequest {
  private PatchCommandType type = PatchCommandType.SET;

  private RavenJToken prevVal;

  private RavenJToken value;

  private PatchRequest[] nested;

  private String name;
  private Integer position;
  private Boolean allPositions;


  public PatchRequest() {
    super();
  }
  public PatchRequest(PatchCommandType type, String name, RavenJToken value) {
    super();
    this.type = type;
    this.name = name;
    this.value = value;
  }
  /**
   * Only valid for collections. Set this property to true if you want to modify all items in an collection.
   */
  public Boolean getAllPositions() {
    return allPositions;
  }

  /**
   * Property/field name to patch.
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the nested operations to perform. This is only valid when the {@link #getType() Type} is {@link net.ravendb.abstractions.data.PatchCommandType#MODIFY PatchCommandType.MODIFY}
   */
  public PatchRequest[] getNested() {
    return nested;
  }

  /**
   * Only valid for collection types. Position in collection to patch.
   */
  public Integer getPosition() {
    return position;
  }

  /**
   * Gets the previous value, which is compared against the current value to verify a
   * change isn't overwriting new values.
   * If the value is null, the operation is always successful
   */
  public RavenJToken getPrevVal() {
    return prevVal;
  }
  /**
   * Type of patch operation.
   */
  public PatchCommandType getType() {
    return type;
  }

  /**
   * New value.
   */
  public RavenJToken getValue() {
    return value;
  }

  /**
   * Only valid for collections. Set this property to true if you want to modify all items in an collection.
   * @param allPositions
   */
  public void setAllPositions(Boolean allPositions) {
    this.allPositions = allPositions;
  }

  /**
   * Property/field name to patch.
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Sets the nested operations to perform. This is only valid when the {@link #getType() Type} is {@link net.ravendb.abstractions.data.PatchCommandType#MODIFY PatchCommandType.MODIFY}
   * @param nested
   */
  public void setNested(PatchRequest[] nested) {
    this.nested = nested;
  }

  /**
   * Only valid for collection types. Position in collection to patch.
   * @param position
   */
  public void setPosition(Integer position) {
    this.position = position;
  }

  /**
   * Sets the previous value, which is compared against the current value to verify a
   * change isn't overwriting new values.
   * If the value is null, the operation is always successful
   */
  public void setPrevVal(RavenJToken prevVal) {
    this.prevVal = prevVal;
  }

  /**
   * Type of patch operation.
   * @param type
   */
  public void setType(PatchCommandType type) {
    this.type = type;
  }

  /**
   * New value.
   * @param value
   */
  public void setValue(RavenJToken value) {
    this.value = value;
  }

  /**
   * Translates this instance to json
   * @return json instance
   */
  public RavenJObject toJson() {
    RavenJObject jObject = new RavenJObject();
    jObject.add("Type", new RavenJValue(SharpEnum.value(type)));
    jObject.add("Value", value);
    jObject.add("Name", new RavenJValue(name));
    if (position != null) {
      jObject.add("Position", new RavenJValue(position));
    }
    if (nested != null) {
      RavenJArray array = new  RavenJArray();
      for (PatchRequest request:  nested) {
        array.add(request.toJson());
      }
      jObject.add("Nested",  array);
    }
    if (allPositions != null) {
      jObject.add("AllPositions", new RavenJValue(allPositions));
    }
    if (prevVal != null) {
      jObject.add("PrevVal", prevVal);
    }
    return jObject;
  }

  /**
   * Create an instance from a json object
   * @param patchRequestJson
   * @return PatchRequest
   */
  public static PatchRequest fromJson(RavenJObject patchRequestJson)
  {
    PatchRequest[] nested = null;
    RavenJToken nestedJson = patchRequestJson.value(RavenJToken.class, "Nested");
    if (nestedJson != null && nestedJson.getType() != JTokenType.NULL) {

      List<PatchRequest> nestedList = new ArrayList<>();

      RavenJArray ravenJArray = patchRequestJson.value(RavenJArray.class, "Nested");
      for (RavenJToken token: ravenJArray) {
        nestedList.add(fromJson((RavenJObject) token));
      }
      nested = nestedList.toArray(new PatchRequest[0]);
    }

    PatchRequest request = new PatchRequest();
    request.setType(SharpEnum.fromValue(patchRequestJson.value(String.class, "Type"), PatchCommandType.class));
    request.setName(patchRequestJson.value(String.class, "Name"));
    request.setNested(nested);
    request.setPosition(patchRequestJson.value(Integer.class, "Position"));
    request.setAllPositions(patchRequestJson.value(Boolean.class, "AllPositions"));
    request.setPrevVal(patchRequestJson.get("PrevVal"));
    request.setValue(patchRequestJson.get("Value"));

    return request;
  }
}
