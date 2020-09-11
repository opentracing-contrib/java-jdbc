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

import static io.opentracing.contrib.jdbc.TestUtil.checkNoEmptyTags;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracerTestUtil;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JdbcTest {
  private static void assertGetDriver(final Connection connection) throws SQLException {
    final String originalURL = connection.getMetaData().getURL();
    final Driver driver = getUnderlyingDriver(originalURL);
    assertEquals("org.h2.Driver", driver.getClass().getName());
  }

  private static Driver getUnderlyingDriver(final String url) throws SQLException {
    final Enumeration<Driver> enumeration = DriverManager.getDrivers();
    while (enumeration.hasMoreElements()) {
      final Driver driver = enumeration.nextElement();
      if (driver.acceptsURL(url) && !(driver instanceof TracingDriver)) {
        return driver;
      }
    }
    return null;
  }

  private static final MockTracer mockTracer = new MockTracer();

  @BeforeClass
  public static void init() {
    GlobalTracerTestUtil.setGlobalTracerUnconditionally(mockTracer);
  }

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void testPassTracingUrl() throws Exception {
    TracingDriver.setInterceptorMode(false);
    try (Connection connection = DriverManager.getConnection("jdbc:tracing:h2:mem:jdbc")) {
      Statement statement = connection.createStatement();
      statement.executeUpdate("CREATE TABLE employer (id INTEGER)");
      assertGetDriver(connection);
    }

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(3, spans.size());
    checkNoEmptyTags(spans);
  }

  @Test
  public void testFailTracingUrl() throws Exception {
    try (Connection connection = DriverManager.getConnection("jdbc:tracing:h2:mem:jdbc")) {
      Statement statement = connection.createStatement();
      try {
        statement.executeUpdate("CREATE TABLE employer (id INTEGER2)");
      } catch (Exception ignore) {
      }
      assertGetDriver(connection);
    }

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(3, spans.size());
    MockSpan span = spans.get(1);
    assertTrue(span.tags().containsKey(Tags.ERROR.getKey()));
    checkNoEmptyTags(spans);
  }

  @Test
  public void testPassOriginalUrl() throws Exception {
    TracingDriver.ensureRegisteredAsTheFirstDriver();
    TracingDriver.setInterceptorMode(true);
    try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:jdbc")) {
      Statement statement = connection.createStatement();
      statement.executeUpdate("CREATE TABLE employer (id INTEGER)");
      assertGetDriver(connection);
    }

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(3, spans.size());
    checkNoEmptyTags(spans);
  }

  @Test
  public void testFailOriginalUrl() throws Exception {
    TracingDriver.ensureRegisteredAsTheFirstDriver();
    TracingDriver.setInterceptorMode(true);
    try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:jdbc")) {
      Statement statement = connection.createStatement();
      try {
        statement.executeUpdate("CREATE TABLE employer (id INTEGER2)");
      } catch (Exception ignore) {
      }
      assertGetDriver(connection);
    }

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(3, spans.size());
    MockSpan span = spans.get(1);
    assertTrue(span.tags().containsKey(Tags.ERROR.getKey()));
    checkNoEmptyTags(spans);
  }

  @Test
  public void testFailInterceptor() throws Exception {
    TracingDriver.setInterceptorMode(false);
    try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:jdbc")) {
      Statement statement = connection.createStatement();
      try {
        statement.executeUpdate("CREATE TABLE employer (id INTEGER2)");
      } catch (Exception ignore) {
      }
      assertGetDriver(connection);
    }

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(0, spans.size());
  }
}
