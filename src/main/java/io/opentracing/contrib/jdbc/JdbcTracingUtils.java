/*
 * Copyright 2017-2020 The OpenTracing Authors
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
import io.opentracing.noop.NoopSpan;
import io.opentracing.tag.BooleanTag;
import io.opentracing.tag.StringTag;
import io.opentracing.tag.Tags;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

class JdbcTracingUtils {

  static final String COMPONENT_NAME = "java-jdbc";

  /**
   * Opentracing standard tag https://github.com/opentracing/specification/blob/master/semantic_conventions.md
   */
  static final StringTag PEER_ADDRESS = new StringTag("peer.address");

  static final BooleanTag SLOW = new BooleanTag("slow");

  static Span buildSpan(String operationName,
      String sql,
      ConnectionInfo connectionInfo,
      boolean withActiveSpanOnly,
      Set<String> ignoreStatements,
      Tracer tracer) {
    if (!JdbcTracing.isTraceEnabled() || (withActiveSpanOnly && tracer.activeSpan() == null)) {
      return NoopSpan.INSTANCE;
    } else if (ignoreStatements != null && ignoreStatements.contains(sql)) {
      return NoopSpan.INSTANCE;
    }

    Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName)
        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

    Span span = spanBuilder.start();
    decorate(span, sql, connectionInfo);

    return span;
  }

  static <E extends Exception> void execute(String operationName,
      CheckedRunnable<E> runnable,
      String sql,
      ConnectionInfo connectionInfo,
      boolean withActiveSpanOnly,
      Set<String> ignoreStatements,
      Tracer tracer) throws E {
    if (!JdbcTracing.isTraceEnabled() || (withActiveSpanOnly && tracer.activeSpan() == null)) {
      runnable.run();
      return;
    }

    final Span span = buildSpan(operationName, sql, connectionInfo, withActiveSpanOnly,
    ignoreStatements, tracer);
    long time = JdbcTracing.getSlowQueryThresholdMs()  > 0 ? System.nanoTime() : 0;
    try (Scope ignored = tracer.activateSpan(span)) {
       runnable.run();
    } catch (Exception e) {
      JdbcTracingUtils.onError(e, span);
      throw e;
    } finally {
      if (JdbcTracing.getSlowQueryThresholdMs() > 0 && System.nanoTime() - time > TimeUnit.MILLISECONDS.toNanos(JdbcTracing.getSlowQueryThresholdMs())) {
        SLOW.set(span, true);
      }
      span.finish();
    }
  }

  static <T, E extends Exception> T call(String operationName,
  CheckedCallable<T, E> callable,
  String sql,
  ConnectionInfo connectionInfo,
  boolean withActiveSpanOnly,
  Set<String> ignoreStatements,
  Tracer tracer) throws E {
    if (!JdbcTracing.isTraceEnabled() || (withActiveSpanOnly && tracer.activeSpan() == null)) {
      return callable.call();
    }

    final Span span = buildSpan(operationName, sql, connectionInfo, withActiveSpanOnly,
        ignoreStatements, tracer);
    long time = JdbcTracing.getSlowQueryThresholdMs()  > 0 ? System.nanoTime() : 0;
    try (Scope ignored = tracer.activateSpan(span)) {
      return callable.call();
    } catch (Exception e) {
      JdbcTracingUtils.onError(e, span);
      throw e;
    } finally {
      if (JdbcTracing.getSlowQueryThresholdMs() > 0 && System.nanoTime() - time > TimeUnit.MILLISECONDS.toNanos(JdbcTracing.getSlowQueryThresholdMs())) {
        SLOW.set(span, true);
      }
      span.finish();
    }
  }

  private static boolean isNotEmpty(CharSequence s) {
    return s != null && !"".contentEquals(s);
  }

  /**
   * Add tags to span. Skip empty tags to avoid reported NPE in tracers.
   */
  private static void decorate(Span span, String sql, ConnectionInfo connectionInfo) {
    Tags.COMPONENT.set(span, COMPONENT_NAME);

    if (isNotEmpty(sql)) {
      Tags.DB_STATEMENT.set(span, sql);
    }
    if (isNotEmpty(connectionInfo.getDbType())) {
      Tags.DB_TYPE.set(span, connectionInfo.getDbType());
    }
    if (isNotEmpty(connectionInfo.getDbPeer())) {
      PEER_ADDRESS.set(span, connectionInfo.getDbPeer());
    }
    if (isNotEmpty(connectionInfo.getDbInstance())) {
      Tags.DB_INSTANCE.set(span, connectionInfo.getDbInstance());
    }
    if (isNotEmpty(connectionInfo.getDbUser())) {
      Tags.DB_USER.set(span, connectionInfo.getDbUser());
    }
    if (isNotEmpty(connectionInfo.getPeerService())) {
      Tags.PEER_SERVICE.set(span, connectionInfo.getPeerService());
    }
  }

  static void onError(Throwable throwable, Span span) {
    Tags.ERROR.set(span, Boolean.TRUE);

    if (throwable != null) {
      span.log(errorLogs(throwable));
    }
  }

  private static Map<String, Object> errorLogs(Throwable throwable) {
    Map<String, Object> errorLogs = new HashMap<>(3);
    errorLogs.put("event", Tags.ERROR.getKey());
    errorLogs.put("error.object", throwable);
    return errorLogs;
  }

  @FunctionalInterface
  interface CheckedRunnable<E extends Throwable> {

    void run() throws E;

  }

  @FunctionalInterface
  interface CheckedCallable<T, E extends Throwable> {

    T call() throws E;

  }
}
