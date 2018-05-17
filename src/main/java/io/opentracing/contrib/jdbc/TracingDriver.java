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


import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TracingDriver implements Driver {

  private static final Driver INSTANCE = new TracingDriver();

  private static final String TRACE_WITH_ACTIVE_SPAN_ONLY = "traceWithActiveSpanOnly";

  private static final String WITH_ACTIVE_SPAN_ONLY = TRACE_WITH_ACTIVE_SPAN_ONLY + "=true";
  public static final String IGNORE_FOR_TRACING_REGEX = "ignoreForTracing=\"((?:\\\\\"|[^\"])*)\"[;]*";

  static {
    try {
      DriverManager.registerDriver(INSTANCE);
    } catch (SQLException e) {
      throw new IllegalStateException("Could not register TracingDriver with DriverManager", e);
    }
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    // if there is no url, we have problems
    if (url == null) {
      throw new SQLException("url is required");
    }

    if (!acceptsURL(url)) {
      return null;
    }

    String realUrl = extractRealUrl(url);
    String dbType = extractDbType(realUrl);
    String dbUser = info.getProperty("user");

    // find the real driver for the URL
    Driver wrappedDriver = findDriver(realUrl);
    Connection connection = wrappedDriver.connect(realUrl, info);

    return new TracingConnection(connection, dbType, dbUser, url.contains(WITH_ACTIVE_SPAN_ONLY),
            extractIgnoredStatements(url));
  }

  @Override
  public boolean acceptsURL(String url) throws SQLException {
    return url != null && url.startsWith("jdbc:tracing:");
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

  private Driver findDriver(String realUrl) throws SQLException {

    Driver wrappedDriver = null;
    for (Driver driver : registeredDrivers()) {
      try {
        if (driver.acceptsURL(realUrl)) {
          wrappedDriver = driver;
          break;
        }
      } catch (SQLException e) {
        // intentionally ignore exception
      }
    }
    if (wrappedDriver == null) {
      throw new SQLException("Unable to find a driver that accepts " + realUrl);
    }
    return wrappedDriver;
  }

  private List<Driver> registeredDrivers() {
    List<Driver> result = new ArrayList<>();
    for (Enumeration<Driver> driverEnumeration = DriverManager.getDrivers();
        driverEnumeration.hasMoreElements(); ) {
      result.add(driverEnumeration.nextElement());
    }
    return result;
  }

  private String extractRealUrl(String url) {
    String extracted = url.startsWith("jdbc:tracing:") ? url.replace("tracing:", "") : url;
    return extracted.replaceAll(TRACE_WITH_ACTIVE_SPAN_ONLY + "=(true|false)[;]*", "")
        .replaceAll(IGNORE_FOR_TRACING_REGEX, "")
        .replaceAll("\\?$", "");
  }

  private String extractDbType(String realUrl) {
    return realUrl.split(":")[1];
  }

  private Set<String> extractIgnoredStatements(String url) {
    final String regex = IGNORE_FOR_TRACING_REGEX;

    final Pattern pattern = Pattern.compile(regex);
    final Matcher matcher = pattern.matcher(url);

    Set<String> results = new HashSet<>();

    while (matcher.find()) {
      String rawValue = matcher.group(1);
      String finalValue = rawValue.replace("\\\"", "\"");
      results.add(finalValue);
    }

    return results;
  }
}
