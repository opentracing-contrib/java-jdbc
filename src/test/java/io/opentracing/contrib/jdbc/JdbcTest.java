/*
 * Copyright 2017-2019 The OpenTracing Authors
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
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JdbcTest {

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
  public void test() throws Exception {
    Connection connection = DriverManager.getConnection("jdbc:tracing:h2:mem:jdbc");
    Statement statement = connection.createStatement();
    statement.executeUpdate("CREATE TABLE employer (id INTEGER)");
    connection.close();

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());
    checkNoEmptyTags(spans);
  }

  @Test
  public void with_error() throws Exception {
    Connection connection = DriverManager.getConnection("jdbc:tracing:h2:mem:jdbc");
    Statement statement = connection.createStatement();
    try {
      statement.executeUpdate("CREATE TABLE employer (id INTEGER2)");
    } catch (Exception ignore) {
    }
    connection.close();

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());
    MockSpan span = spans.get(1);
    assertTrue(span.tags().containsKey(Tags.ERROR.getKey()));
    checkNoEmptyTags(spans);
  }
}
