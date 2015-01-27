package net.ravendb.tests.conflicts;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import net.ravendb.abstractions.json.ConflictsResolver;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJValue;

import org.codehaus.jackson.JsonGenerationException;
import org.junit.Test;

public class ConflictResolverTest {

  @Test
  public void canResolveEmpty() throws JsonGenerationException, IOException {
    ConflictsResolver conflictsResolver = new ConflictsResolver(new RavenJObject(), new RavenJObject());
    assertEquals("{}", conflictsResolver.resolve().getDocument());
  }

  @Test
  public void canResolveIdentical() throws JsonGenerationException, IOException {
    RavenJObject object1 = new RavenJObject();
    object1.add("Name", "Oren");

    RavenJObject object2 = new RavenJObject();
    object2.add("Name", "Oren");

    ConflictsResolver conflictsResolver = new ConflictsResolver(object1, object2);
    assertEquals("{\"Name\":\"Oren\"}", conflictsResolver.resolve().getDocument());
  }

  @Test
  public void canResolveTwoEmptyArrays() throws JsonGenerationException, IOException {
    RavenJObject object1 = new RavenJObject();
    object1.add("Name", new RavenJArray());

    RavenJObject object2 = new RavenJObject();
    object2.add("Name", new RavenJArray());

    ConflictsResolver conflictsResolver = new ConflictsResolver(object1, object2);
    assertEquals("{\"Name\":[]}", conflictsResolver.resolve().getDocument());
  }

  @Test
  public void canResolveOneEmptyArraysAndOneWithValue() throws JsonGenerationException, IOException {
    RavenJObject object1 = new RavenJObject();
    object1.add("Name", new RavenJArray());

    RavenJObject object2 = new RavenJObject();
    object2.add("Name", new RavenJArray(new RavenJValue(1)));

    ConflictsResolver conflictsResolver = new ConflictsResolver(object1, object2);
    assertEquals("{\"Name\"/* >>>> auto merged array start */:[1]/* <<<< auto merged array end */}", conflictsResolver.resolve().getDocument());
  }

  @SuppressWarnings("boxing")
  @Test
  public void canMergeAdditionalProperties() throws JsonGenerationException, IOException {
    RavenJObject object1 = new RavenJObject();
    object1.add("Name", "Oren");

    RavenJObject object2 = new RavenJObject();
    object2.add("Age", 2);

    ConflictsResolver conflictsResolver = new ConflictsResolver(object1, object2);
    assertEquals("{\"Name\":\"Oren\",\"Age\":2}", conflictsResolver.resolve().getDocument());
  }

  @Test
  public void canDetectAndSuggestOptionsForConflict_SimpleProp() throws JsonGenerationException, IOException {
    RavenJObject object1 = new RavenJObject();
    object1.add("Name", "Oren");

    RavenJObject object2 = new RavenJObject();
    object2.add("Name", "Ayende");

    ConflictsResolver conflictsResolver = new ConflictsResolver(object1, object2);
    assertEquals("{\"Name\"/* >>>> conflict start */:[\"Oren\",\"Ayende\"]/* <<<< conflict end */}", conflictsResolver.resolve().getDocument());
  }

  @Test
  public void canMergeProperties_Nested() throws JsonGenerationException, IOException {
    RavenJObject nested1 = new RavenJObject();
    nested1.add("First", "Oren");
    RavenJObject object1 = new RavenJObject();
    object1.add("Name", nested1);

    RavenJObject nested2 = new RavenJObject();
    nested2.add("Last", "Eini");

    RavenJObject object2 = new RavenJObject();
    object2.add("Name", nested2);

    ConflictsResolver conflictsResolver = new ConflictsResolver(object1, object2);
    assertEquals("{\"Name\":{\"Last\":\"Eini\",\"First\":\"Oren\"}}", conflictsResolver.resolve().getDocument());
  }

  @Test
  public void canDetectConflict_DifferentValues() throws JsonGenerationException, IOException {
    RavenJObject nested1 = new RavenJObject();
    nested1.add("First", "Oren");
    RavenJObject object1 = new RavenJObject();
    object1.add("Name", nested1);

    RavenJObject object2 = new RavenJObject();
    object2.add("Name", "Eini");

    ConflictsResolver conflictsResolver = new ConflictsResolver(object1, object2);
    assertEquals("{\"Name\"/* >>>> conflict start */:[{\"First\":\"Oren\"},\"Eini\"]/* <<<< conflict end */}", conflictsResolver.resolve().getDocument());
  }

  @Test
  public void canDetectAndSuggestOptionsForConflict_NestedProp() throws JsonGenerationException, IOException {
    RavenJObject object1 = new RavenJObject();
    object1.add("Name", "Oren");

    RavenJObject object2 = new RavenJObject();
    object2.add("Name", "Ayende");

    ConflictsResolver conflictsResolver = new ConflictsResolver(object1, object2);
    assertEquals("{\"Name\"/* >>>> conflict start */:[\"Oren\",\"Ayende\"]/* <<<< conflict end */}", conflictsResolver.resolve().getDocument());
  }

  @Test
  public void canMergeArrays() throws JsonGenerationException, IOException {
    RavenJObject object1 = new RavenJObject();
    object1.add("Nicks", new RavenJArray(new RavenJValue("Oren")));

    RavenJObject object2 = new RavenJObject();
    object2.add("Nicks", new RavenJArray(new RavenJValue("Ayende")));

    ConflictsResolver conflictsResolver = new ConflictsResolver(object1, object2);
    assertEquals("{\"Nicks\"/* >>>> auto merged array start */:[\"Oren\",\"Ayende\"]/* <<<< auto merged array end */}", conflictsResolver.resolve().getDocument());
  }

  @SuppressWarnings("boxing")
  @Test
  public void canMergeArrays_SameStart() throws JsonGenerationException, IOException {
    RavenJObject object1 = new RavenJObject();
    object1.add("Comments", new RavenJArray(Arrays.asList(1,2,4)));

    RavenJObject object2 = new RavenJObject();
    object2.add("Comments", new RavenJArray(Arrays.asList(1,2,5)));

    ConflictsResolver conflictsResolver = new ConflictsResolver(object1, object2);
    assertEquals("{\"Comments\"/* >>>> auto merged array start */:[1,2,4,5]/* <<<< auto merged array end */}", conflictsResolver.resolve().getDocument());
  }

}
