package io.opentracing.contrib.jdbc;

import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;


class JdbcTracingUtils {

  static final String COMPONENT_NAME = "java-jdbc";

  static ActiveSpan buildSpan(String operationName, String sql, String dbType, String dbUser) {
    Tracer.SpanBuilder spanBuilder = GlobalTracer.get().buildSpan(operationName)
        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

    ActiveSpan span = spanBuilder.startActive();
    decorate(span, sql, dbType, dbUser);

    return span;
  }

  private static void decorate(ActiveSpan span, String sql, String dbType, String dbUser) {
    Tags.COMPONENT.set(span, COMPONENT_NAME);
    Tags.DB_STATEMENT.set(span, sql);
    Tags.DB_TYPE.set(span, dbType);
    if (dbUser != null) {
      Tags.DB_USER.set(span, dbUser);
    }
  }
}
