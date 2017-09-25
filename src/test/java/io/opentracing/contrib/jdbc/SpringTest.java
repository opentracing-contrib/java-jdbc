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


import static io.opentracing.contrib.jdbc.TestUtil.checkSameTrace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.opentracing.ActiveSpan;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class SpringTest {

  private static final MockTracer mockTracer = new MockTracer(new ThreadLocalActiveSpanSource(),
      MockTracer.Propagator.TEXT_MAP);

  @BeforeClass
  public static void init() {
    GlobalTracer.register(mockTracer);
  }

  @Before
  public void before() throws Exception {
    mockTracer.reset();
  }

  @Test
  public void spring() throws SQLException {
    BasicDataSource dataSource = getDataSource(false);

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("CREATE TABLE employee (id INTEGER)");

    dataSource.close();

    List<MockSpan> finishedSpans = mockTracer.finishedSpans();
    assertEquals(1, finishedSpans.size());
    MockSpan mockSpan = finishedSpans.get(0);

    assertEquals(Tags.SPAN_KIND_CLIENT, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
    assertEquals(JdbcTracingUtils.COMPONENT_NAME, mockSpan.tags().get(Tags.COMPONENT.getKey()));
    assertNotNull(mockSpan.tags().get(Tags.DB_STATEMENT.getKey()));
    assertEquals("h2", mockSpan.tags().get(Tags.DB_TYPE.getKey()));
    assertEquals("sa", mockSpan.tags().get(Tags.DB_USER.getKey()));
    assertEquals(0, mockSpan.generatedErrors().size());

    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void spring_skip_new_spans() throws Exception {
    BasicDataSource dataSource = getDataSource(true);

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("CREATE TABLE skip_new_spans (id INTEGER)");

    dataSource.close();

    List<MockSpan> finishedSpans = mockTracer.finishedSpans();
    assertEquals(0, finishedSpans.size());
  }

  @Test
  public void spring_with_parent() throws Exception {
    try (ActiveSpan activeSpan = mockTracer.buildSpan("parent").startActive()) {
      BasicDataSource dataSource = getDataSource(false);

      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      jdbcTemplate.execute("CREATE TABLE with_parent_1 (id INTEGER)");
      jdbcTemplate.execute("CREATE TABLE with_parent_2 (id INTEGER)");

      dataSource.close();
    }

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(3, spans.size());

    checkSameTrace(spans);
  }

  @Test
  public void spring_with_parent_skip_new_spans() throws Exception {
    try (ActiveSpan activeSpan = mockTracer.buildSpan("parent").startActive()) {
      BasicDataSource dataSource = getDataSource(true);

      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      jdbcTemplate.execute("CREATE TABLE with_parent_skip_1 (id INTEGER)");
      jdbcTemplate.execute("CREATE TABLE with_parent_skip_2 (id INTEGER)");

      dataSource.close();
    }

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(3, spans.size());

    checkSameTrace(spans);
  }

  private BasicDataSource getDataSource(boolean traceWithActiveSpanOnly) {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName("io.opentracing.contrib.jdbc.TracingDriver");
    dataSource
        .setUrl("jdbc:tracing:h2:mem:spring?traceWithActiveSpanOnly=" + traceWithActiveSpanOnly);
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    return dataSource;
  }

}
