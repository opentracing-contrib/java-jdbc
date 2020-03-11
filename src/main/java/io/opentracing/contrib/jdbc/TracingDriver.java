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

import static io.opentracing.contrib.jdbc.JdbcTracingUtils.*;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.common.WrapperProxy;
import io.opentracing.contrib.jdbc.parser.URLParser;
import io.opentracing.util.GlobalTracer;

public class TracingDriver implements Driver {

  private static final Driver INSTANCE = new TracingDriver();

  protected static final String TRACE_WITH_ACTIVE_SPAN_ONLY = "traceWithActiveSpanOnly";

  protected static final String WITH_ACTIVE_SPAN_ONLY = TRACE_WITH_ACTIVE_SPAN_ONLY + "=true";

  public static final String IGNORE_FOR_TRACING_REGEX = "ignoreForTracing=\"((?:\\\\\"|[^\"])*)\"[;]*";

  protected static final Pattern PATTERN_FOR_IGNORING = Pattern.compile(IGNORE_FOR_TRACING_REGEX);

  static {
    load();
  }

  /**
   * Load the {@code TracingDriver} into the {@link DriverManager}.<br> This method has the
   * following behavior:
   * <ol>
   * <li>Deregister all previously registered drivers.</li>
   * <li>Load {@code TracingDriver} as the first driver.</li>
   * <li>Reregister all drivers that were just deregistered.</li>
   * </ol>
   *
   * @return The singleton instance of the {@code TracingDriver}.
   */
  public synchronized static Driver load() {
    try {
      final Enumeration<Driver> enumeration = DriverManager.getDrivers();
      List<Driver> drivers = null;
      for (int i = 0; enumeration.hasMoreElements(); ++i) {
        final Driver driver = enumeration.nextElement();
        if (i == 0) {
          if (driver == INSTANCE) {
            return driver;
          }

          drivers = new ArrayList<>();
        }

        drivers.add(driver);
      }

      // Deregister all drivers
      if (drivers != null) {
        for (final Driver driver : drivers) {
          DriverManager.deregisterDriver(driver);
        }
      }

      // Register TracingDriver as the first driver
      DriverManager.registerDriver(INSTANCE);

      // Reregister all drivers
      if (drivers != null) {
        for (final Driver driver : drivers) {
          DriverManager.registerDriver(driver);
        }
      }

      return INSTANCE;
    } catch (SQLException e) {
      throw new IllegalStateException("Could not register TracingDriver with DriverManager", e);
    }
  }

    private static boolean traceEnabled = true;

    /**
     * Sets the {@code traceEnabled} property to enable or disable traces.
     *
     * @param traceEnabled The {@code traceEnabled} value.
     */
    public static void setTraceEnabled(boolean traceEnabled) {
        TracingDriver.traceEnabled = traceEnabled;
    }

    public static boolean isTraceEnabled() {
        return TracingDriver.traceEnabled;
    }

    private static boolean interceptorMode = false;

  /**
   * Turns "interceptor mode" on or off.
   *
   * @param interceptorMode The {@code interceptorMode} value.
   */
  public static void setInterceptorMode(final boolean interceptorMode) {
    TracingDriver.interceptorMode = interceptorMode;
  }

  private static boolean withActiveSpanOnly;

  /**
   * Sets the {@code withActiveSpanOnly} property for "interceptor mode".
   *
   * @param withActiveSpanOnly The {@code withActiveSpanOnly} value.
   */
  public static void setInterceptorProperty(final boolean withActiveSpanOnly) {
    TracingDriver.withActiveSpanOnly = withActiveSpanOnly;
  }

  private static Set<String> ignoreStatements;

  /**
   * Sets the {@code ignoreStatements} property for "interceptor mode".
   *
   * @param ignoreStatements The {@code ignoreStatements} value.
   */
  public static void setInterceptorProperty(final Set<String> ignoreStatements) {
    TracingDriver.ignoreStatements = ignoreStatements;
  }

  protected Tracer tracer;

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    // if there is no url, we have problems
    if (url == null) {
      throw new SQLException("url is required");
    }

    final Set<String> ignoreStatements;
    final boolean withActiveSpanOnly;
    if (interceptorMode) {
      withActiveSpanOnly = TracingDriver.withActiveSpanOnly;
      ignoreStatements = TracingDriver.ignoreStatements;
    } else if (acceptsURL(url)) {
      withActiveSpanOnly = url.contains(WITH_ACTIVE_SPAN_ONLY);
      ignoreStatements = extractIgnoredStatements(url);
    } else {
      return null;
    }

    url = extractRealUrl(url);

    // find the real driver for the URL
    final Driver wrappedDriver = findDriver(url);

    final Tracer currentTracer = getTracer();
    final ConnectionInfo connectionInfo = URLParser.parser(url);
    final Span span = buildSpan("AcquireConnection", "", connectionInfo, withActiveSpanOnly,
        Collections.<String>emptySet(), currentTracer);
    final Connection connection;
    try (Scope ignored = currentTracer.activateSpan(span)) {
      connection = wrappedDriver.connect(url, info);
    } finally {
      span.finish();
    }

    return WrapperProxy
        .wrap(connection, new TracingConnection(connection, connectionInfo, withActiveSpanOnly,
            ignoreStatements, currentTracer));
  }

  @Override
  public boolean acceptsURL(String url) throws SQLException {
    return url != null && (
        url.startsWith(getUrlPrefix()) ||
        (interceptorMode && url.startsWith("jdbc:"))
    );
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
    return findDriver(url).getPropertyInfo(url, info);
  }

  @Override
  public int getMajorVersion() {
    // There is no way to get it from wrapped driver
    return 1;
  }

  @Override
  public int getMinorVersion() {
    // There is no way to get it from wrapped driver
    return 0;
  }

  @Override
  public boolean jdbcCompliant() {
    return true;
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    // There is no way to get it from wrapped driver
    return null;
  }

  public void setTracer(Tracer tracer) {
    this.tracer = tracer;
  }

  protected String getUrlPrefix() {
    return "jdbc:tracing:";
  }

  protected Driver findDriver(String realUrl) throws SQLException {
    if (realUrl == null || realUrl.trim().length() == 0) {
      throw new IllegalArgumentException("url is required");
    }

    for (Driver candidate : Collections.list(DriverManager.getDrivers())) {
      try {
        if (!(candidate instanceof TracingDriver) && candidate.acceptsURL(realUrl)) {
          return candidate;
        }
      } catch (SQLException ignored) {
        // intentionally ignore exception
      }
    }

    throw new SQLException("Unable to find a driver that accepts url: " + realUrl);
  }

  protected String extractRealUrl(String url) {
    String extracted = url.startsWith(getUrlPrefix()) ? url.replace(getUrlPrefix(), "jdbc:") : url;
    return extracted.replaceAll(TRACE_WITH_ACTIVE_SPAN_ONLY + "=(true|false)[;]*", "")
        .replaceAll(IGNORE_FOR_TRACING_REGEX, "")
        .replaceAll("\\?$", "");
  }

  protected Set<String> extractIgnoredStatements(String url) {

    final Matcher matcher = PATTERN_FOR_IGNORING.matcher(url);

    Set<String> results = new HashSet<>(8);

    while (matcher.find()) {
      String rawValue = matcher.group(1);
      String finalValue = rawValue.replace("\\\"", "\"");
      results.add(finalValue);
    }

    return results;
  }

  Tracer getTracer() {
    if (tracer == null) {
      return GlobalTracer.get();
    }
    return tracer;
  }
}
