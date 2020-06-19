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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Test;

public class TracingDataSourceTest {
  @Test
  public void traces_acquiring_connection() throws Exception {
    final BasicDataSource dataSource = getDataSource();
    final MockTracer mockTracer = new MockTracer();
    try (final TracingDataSource tracingDataSource = new TracingDataSource(mockTracer, dataSource)) {
      try (final Connection connection = tracingDataSource.getConnection()) {
        assertFalse(mockTracer.finishedSpans().isEmpty());
      }
    }
  }

  @Test
  public void sets_error() throws Exception {
    final BasicDataSource dataSource = getErroneousDataSource();
    final MockTracer mockTracer = new MockTracer();
    try (final TracingDataSource tracingDataSource = new TracingDataSource(mockTracer, dataSource)) {
      try (final Connection connection = tracingDataSource.getConnection()) {
        assertNull("Get connection", connection);
      } catch (SQLException ignored) {
      }
    }

    assertFalse(mockTracer.finishedSpans().isEmpty());
    MockSpan finishedSpan = mockTracer.finishedSpans().get(0);
    assertTrue("Span contains error tag", finishedSpan.tags().containsKey(Tags.ERROR.getKey()));
  }

  @Test(expected = SQLException.class)
  public void rethrows_any_error() throws Exception {
    final BasicDataSource dataSource = getErroneousDataSource();
    final MockTracer mockTracer = new MockTracer();
    try (final TracingDataSource tracingDataSource = new TracingDataSource(mockTracer, dataSource)) {
      tracingDataSource.getConnection();
    }
  }

  private static BasicDataSource getDataSource() {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setUrl("jdbc:h2:mem:dataSourceTest");
    return dataSource;
  }

  private static BasicDataSource getErroneousDataSource() {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setUrl("jdbc:invalid");
    return dataSource;
  }
}