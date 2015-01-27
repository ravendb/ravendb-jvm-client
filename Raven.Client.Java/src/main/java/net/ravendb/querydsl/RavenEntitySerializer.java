package net.ravendb.querydsl;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.mysema.codegen.CodeWriter;
import com.mysema.codegen.model.ClassType;
import com.mysema.codegen.model.Type;
import com.mysema.codegen.model.TypeCategory;
import com.mysema.query.codegen.EntitySerializer;
import com.mysema.query.codegen.EntityType;
import com.mysema.query.codegen.Property;
import com.mysema.query.codegen.SerializerConfig;
import com.mysema.query.codegen.TypeMappings;
import com.mysema.query.types.expr.SimpleExpression;
import com.mysema.query.types.path.ArrayPath;
import com.mysema.query.types.path.ListPath;
import com.mysema.query.types.path.MapPath;

public class RavenEntitySerializer extends EntitySerializer {

  public RavenEntitySerializer(TypeMappings mappings, Collection<String> keywords) {
    super(mappings, keywords);
  }

  @Override
  protected void outro(EntityType model, CodeWriter writer) throws IOException {
    writeCreateListMethod(model, writer);
    writeCreateArrayMethod(model, writer);
    writeCreateMapMethod(model, writer);
    super.outro(model, writer);
  }

  @SuppressWarnings("static-method")
  private boolean hasListProperty(EntityType model) {
    for (Property property : model.getProperties()) {
      if (property.getType().getFullName().equals(List.class.getName())) {
        return true;
      }
    }

    return false;
  }

  @SuppressWarnings("static-method")
  private boolean hasArrayProperty(EntityType model) {
    for (Property property : model.getProperties()) {
      if (TypeCategory.ARRAY.equals(property.getType().getCategory())) {
        return true;
      }
    }

    return false;
  }

  @SuppressWarnings("static-method")
  private boolean hasMapProperty(EntityType model) {
    for (Property property : model.getProperties()) {
      if (TypeCategory.MAP.equals(property.getType().getCategory())) {
        return true;
      }
    }

    return false;
  }

  private void writeCreateListMethod(EntityType model, CodeWriter writer) throws IOException {

    if (!hasListProperty(model) ) {
      return;
    }

    writer.suppressWarnings("all");
    writer.append("    protected <A, E extends SimpleExpression<? super A>> RavenList<A, E> createList(String property, Class<? super A> type, Class<? super E> queryType, PathInits inits) {");
    writer.nl();
    writer.append("       return add(new RavenList<A, E>(type, (Class) queryType, forProperty(property), inits));");
    writer.nl();
    writer.append("    }");
    writer.nl();
  }

  private void writeCreateArrayMethod(EntityType model, CodeWriter writer) throws IOException {

    if (!hasArrayProperty(model) ) {
      return;
    }

    writer.suppressWarnings("all");
    writer.append("    protected <A> RavenArray<A> createArray(String property, Class<? super A[]> type) {");
    writer.nl();
    writer.append("        return ((RavenArray)add(new RavenArray(type, forProperty(property))));");
    writer.nl();
    writer.append("    }");
    writer.nl();
  }

  private void writeCreateMapMethod(EntityType model, CodeWriter writer) throws IOException {

    if (!hasMapProperty(model) ) {
      return;
    }



    writer.suppressWarnings("all");
    writer.append("    protected <K, V, E extends SimpleExpression<? super V>> RavenMap<K, V, E> createMap(String property, Class<? super K> key, Class<? super V> value, Class<? super E> queryType) {");
    writer.nl();
    writer.append("        return ((RavenMap)add(new RavenMap(key, value, queryType, forProperty(property))));");
    writer.nl();
    writer.append("    }");
    writer.nl();
  }


  @Override
  protected void introImports(CodeWriter writer, SerializerConfig config, EntityType model) throws IOException {
    super.introImports(writer, config, model);
    if (hasListProperty(model)) {
      writer.imports(SimpleExpression.class, RavenList.class);
    }
    if (hasArrayProperty(model)) {
      writer.imports(RavenArray.class);
    }
    if (hasMapProperty(model)) {
      writer.imports(SimpleExpression.class, RavenMap.class);
    }
  }

  @Override
  protected void serialize(EntityType model, Property field, Type type, CodeWriter writer, String factoryMethod, String... args) throws IOException {
    if (type instanceof ClassType) {
      ClassType classType = (ClassType) type;
      if (classType.getJavaClass().equals(ListPath.class)) {
        type = new ClassType(type.getCategory(), RavenList.class, type.getParameters());
      } else if (classType.getJavaClass().equals(ArrayPath.class)) {
        type = new ClassType(type.getCategory(), RavenArray.class, type.getParameters());
      } else if (classType.getJavaClass().equals(MapPath.class)){
        type = new ClassType(type.getCategory(), RavenMap.class, type.getParameters());
      }
    }
    super.serialize(model, field, type, writer, factoryMethod, args);
  }


}
