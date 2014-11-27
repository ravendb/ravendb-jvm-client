package net.ravendb.abstractions.extensions;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import net.ravendb.abstractions.extensions.ExpressionExtensions;
import net.ravendb.samples.QDeveloper;

import org.junit.Test;

import com.mysema.query.annotations.QueryEntity;


public class ExpressionExtensionsTest {

  @Test
  public void testToPropertyPath() {
    QDeveloper developer = QDeveloper.developer;
    assertEquals("Nick", ExpressionExtensions.toPropertyPath(developer.nick, '_'));
    assertEquals("MainSkill_Name", ExpressionExtensions.toPropertyPath(developer.mainSkill().name, '_'));
    assertEquals("Skills_,Name", ExpressionExtensions.toPropertyPath(developer.skills.select().name, '_'));

    QExpressionExtensionsTest_PersonWithAttribute p = QExpressionExtensionsTest_PersonWithAttribute.personWithAttribute;
    assertEquals("Attributes.$Keys", ExpressionExtensions.toPropertyPath(p.attributes.keys()));
    assertEquals("Attributes.$Values", ExpressionExtensions.toPropertyPath(p.attributes.values()));
    assertEquals("Attributes.$Values,Ref", ExpressionExtensions.toPropertyPath(p.attributes.values().select().ref));
  }

  @QueryEntity
  public static class PersonWithAttribute {
    private String id;
    private String name;
    private Map<String, Attribute> attributes;

    public String getId() {
      return id;
    }
    public void setId(String id) {
      this.id = id;
    }
    public String getName() {
      return name;
    }
    public void setName(String name) {
      this.name = name;
    }
    public Map<String, Attribute> getAttributes() {
      return attributes;
    }
    public void setAttributes(Map<String, Attribute> attributes) {
      this.attributes = attributes;
    }
  }

  @QueryEntity
  public static class Attribute {
    private String ref;

    public String getRef() {
      return ref;
    }
    public void setRef(String ref) {
      this.ref = ref;
    }
  }

}
