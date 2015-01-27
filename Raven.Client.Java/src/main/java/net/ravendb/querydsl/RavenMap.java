package net.ravendb.querydsl;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.PathType;
import com.mysema.query.types.expr.SimpleExpression;
import com.mysema.query.types.path.MapPath;
import com.mysema.query.types.path.SimplePath;

public class RavenMap<K, V, Q extends SimpleExpression<? super V>> extends MapPath<K, V, Q> {

  private Class<Q> queryType;

  public RavenMap(Class<? super K> keyType, Class<? super V> valueType, Class<Q> queryType, Path<?> parent,
    String property) {
    super(keyType, valueType, queryType, parent, property);
    this.queryType = queryType;
  }

  public RavenMap(Class<? super K> keyType, Class<? super V> valueType, Class<Q> queryType, PathMetadata<?> metadata) {
    super(keyType, valueType, queryType, metadata);
    this.queryType = queryType;
  }

  public RavenMap(Class<? super K> keyType, Class<? super V> valueType, Class<Q> queryType, String variable) {
    super(keyType, valueType, queryType, variable);
    this.queryType = queryType;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public RavenList<K, SimpleExpression<? super K>> keys() {
    return new RavenList<>(getKeyType(), (Class)SimplePath.class, new PathMetadata<>((Path<?>)mixin, "$Keys", PathType.PROPERTY));
  }

  public RavenList<V, Q> values() {
    return new RavenList<>(getValueType(), queryType, new PathMetadata<>((Path<?>)mixin, "$Values", PathType.PROPERTY));
  }

}
