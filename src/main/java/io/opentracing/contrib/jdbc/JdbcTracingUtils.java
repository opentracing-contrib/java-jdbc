/*
 * Copyright 2017-2018 The OpenTracing Authors
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

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopScopeManager.NoopScope;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


class JdbcTracingUtils {

  static final String COMPONENT_NAME = "java-jdbc";

  static Scope buildScope(String operationName,
      String sql,
      ConnectionInfo connectionInfo,
      boolean withActiveSpanOnly,
      Set<String> ignoredStatements,
      Tracer tracer) {
    Tracer currentTracer = getNullsafeTracer(tracer);
    if (withActiveSpanOnly && currentTracer.activeSpan() == null) {
      return NoopScope.INSTANCE;
    } else if (ignoredStatements != null && ignoredStatements.contains(sql)) {
      return NoopScope.INSTANCE;
    }

    Tracer.SpanBuilder spanBuilder = currentTracer.buildSpan(operationName)
        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

    Scope scope = spanBuilder.startActive(true);
    decorate(scope.span(), sql, connectionInfo);

    return scope;
  }

  private static Tracer getNullsafeTracer(final Tracer tracer) {
    if (tracer == null) {
      return GlobalTracer.get();
    }
    return tracer;
  }

  private static void decorate(Span span,
      String sql,
      ConnectionInfo connectionInfo) {
    Tags.COMPONENT.set(span, COMPONENT_NAME);
    Tags.DB_STATEMENT.set(span, sql);
    if (connectionInfo.getDbType() != null) {
      Tags.DB_TYPE.set(span, connectionInfo.getDbType());
    }
    if (connectionInfo.getDbIp() != null) {
      Tags.PEER_HOST_IPV4.set(span, connectionInfo.getDbIp());
    }
    if (connectionInfo.getDbInstance() != null) {
      Tags.DB_INSTANCE.set(span, connectionInfo.getDbInstance());
    }
    if (connectionInfo.getDbUser() != null) {
      Tags.DB_USER.set(span, connectionInfo.getDbUser());
    }
    if (connectionInfo.getDbPort() != null) {
      Tags.PEER_PORT.set(span, connectionInfo.getDbPort());
    }
  }

  static void onError(Throwable throwable, Span span) {
    Tags.ERROR.set(span, Boolean.TRUE);

    if (throwable != null) {
      span.log(errorLogs(throwable));
    }
  }

  private static Map<String, Object> errorLogs(Throwable throwable) {
    Map<String, Object> errorLogs = new HashMap<>(2);
    errorLogs.put("event", Tags.ERROR.getKey());
    errorLogs.put("error.object", throwable);
    return errorLogs;
  }
}
