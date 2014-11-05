package net.ravendb.querydsl;

import java.util.Collection;

import net.ravendb.abstractions.LinqOps;

import com.mysema.query.types.ConstantImpl;
import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.expr.BooleanOperation;
import com.mysema.query.types.path.ArrayPath;
import com.mysema.query.types.path.SimplePath;


public class RavenArray<E> extends ArrayPath<E> {

  public RavenArray(Class<? super E[]> type, Path<?> parent, String property) {
    super(type, parent, property);
  }

  public RavenArray(Class<? super E[]> type, PathMetadata<?> metadata) {
    super(type, metadata);
  }

  public RavenArray(Class<? super E[]> type, String variable) {
    super(type, variable);
  }

  public BooleanExpression containsAny(Collection<E> items) {
    return BooleanOperation.create(LinqOps.Ops.CONTAINS_ANY, mixin, new ConstantImpl<>(items));
  }

  public BooleanExpression containsAll(Collection<E> items) {
    return BooleanOperation.create(LinqOps.Ops.CONTAINS_ALL, mixin, new ConstantImpl<>(items));
  }

  public BooleanExpression any(BooleanExpression boolExpr) {
    return BooleanOperation.create(LinqOps.Query.ANY, mixin, boolExpr);
  }

  /**
   * Use can't use select on arrays. Use List instead.
   * @return
   */
  @Deprecated
  public SimplePath<E> select() {
    throw new IllegalStateException("Use can't use select on arrays. Use List instead.");
  }


}
