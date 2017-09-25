/*
 * Copyright 2017 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.jdbc;

import io.opentracing.ActiveSpan;
import io.opentracing.NoopActiveSpanSource.NoopActiveSpan;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;


class JdbcTracingUtils {

  static final String COMPONENT_NAME = "java-jdbc";

  static ActiveSpan buildSpan(String operationName, String sql, String dbType, String dbUser,
      boolean withActiveSpanOnly) {
    if (withActiveSpanOnly && GlobalTracer.get().activeSpan() == null) {
      return NoopActiveSpan.INSTANCE;
    }

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
