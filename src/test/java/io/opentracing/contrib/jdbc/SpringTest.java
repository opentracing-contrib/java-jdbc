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
import static io.opentracing.contrib.jdbc.TestUtil.checkSameTrace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.opentracing.Scope;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracerTestUtil;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

public class SpringTest {

  private static final MockTracer mockTracer = new MockTracer();

  private final int DB_CONNECTION_SPAN_COUNT = 2;

  @BeforeClass
  public static void init() {
    GlobalTracerTestUtil.setGlobalTracerUnconditionally(mockTracer);
  }

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void batch() throws SQLException {
    BasicDataSource dataSource = getDataSource(false);

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("CREATE TABLE batch (id INTEGER)");

    final List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);
    jdbcTemplate.batchUpdate("INSERT INTO batch (id) VALUES (?)",
        new BatchPreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
            preparedStatement.setInt(1, ids.get(i));
          }

          @Override
          public int getBatchSize() {
            return ids.size();
          }
        }
    );

    dataSource.close();

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(DB_CONNECTION_SPAN_COUNT + 2, spans.size());

    for (MockSpan span : spans.subList(DB_CONNECTION_SPAN_COUNT, spans.size() - 1)) {
      assertEquals(Tags.SPAN_KIND_CLIENT, span.tags().get(Tags.SPAN_KIND.getKey()));
      assertEquals(JdbcTracingUtils.COMPONENT_NAME, span.tags().get(Tags.COMPONENT.getKey()));
      assertThat(span.tags().get(Tags.DB_STATEMENT.getKey()).toString()).isNotEmpty();
      assertEquals("h2", span.tags().get(Tags.DB_TYPE.getKey()));
      assertEquals("spring", span.tags().get(Tags.DB_INSTANCE.getKey()));
      assertEquals("localhost:-1", span.tags().get("peer.address"));
      assertEquals(0, span.generatedErrors().size());
    }

    assertNull(mockTracer.activeSpan());
    checkNoEmptyTags(spans);
  }

  @Test
  public void spring() throws SQLException {
    BasicDataSource dataSource = getDataSource(false);

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("CREATE TABLE employee (id INTEGER)");

    dataSource.close();

    List<MockSpan> finishedSpans = mockTracer.finishedSpans();
    assertEquals(DB_CONNECTION_SPAN_COUNT + 1, finishedSpans.size());
    MockSpan mockSpan = finishedSpans.get(DB_CONNECTION_SPAN_COUNT);

    assertEquals(Tags.SPAN_KIND_CLIENT, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
    assertEquals(JdbcTracingUtils.COMPONENT_NAME, mockSpan.tags().get(Tags.COMPONENT.getKey()));
    assertThat(mockSpan.tags().get(Tags.DB_STATEMENT.getKey()).toString()).isNotEmpty();
    assertEquals("h2", mockSpan.tags().get(Tags.DB_TYPE.getKey()));
    assertEquals("spring", mockSpan.tags().get(Tags.DB_INSTANCE.getKey()));
    assertEquals("localhost:-1", mockSpan.tags().get("peer.address"));

    assertEquals(0, mockSpan.generatedErrors().size());

    assertNull(mockTracer.activeSpan());
    checkNoEmptyTags(finishedSpans);
  }

  @Test
  public void spring_active_span_only() throws Exception {
    BasicDataSource dataSource = getDataSource(true);

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("CREATE TABLE skip_new_spans (id INTEGER)");

    dataSource.close();

    List<MockSpan> finishedSpans = mockTracer.finishedSpans();
    assertEquals(0, finishedSpans.size());
    checkNoEmptyTags(finishedSpans);
  }

  @Test
  public void spring_with_parent() throws Exception {
    final MockSpan parent = mockTracer.buildSpan("parent").start();
    try (Scope ignored = mockTracer.activateSpan(parent)) {
      BasicDataSource dataSource = getDataSource(false);

      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      jdbcTemplate.execute("CREATE TABLE with_parent_1 (id INTEGER)");
      jdbcTemplate.execute("CREATE TABLE with_parent_2 (id INTEGER)");

      dataSource.close();
    }
    parent.finish();

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(DB_CONNECTION_SPAN_COUNT + 3, spans.size());

    checkSameTrace(spans);
    checkNoEmptyTags(spans);
  }

  @Test
  public void spring_with_parent_and_active_span_only() throws Exception {
    final MockSpan parent = mockTracer.buildSpan("parent").start();
    try (Scope ignored = mockTracer.activateSpan(parent)) {
      BasicDataSource dataSource = getDataSource(true);

      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      jdbcTemplate.execute("CREATE TABLE with_parent_skip_1 (id INTEGER)");
      jdbcTemplate.execute("CREATE TABLE with_parent_skip_2 (id INTEGER)");

      dataSource.close();
    }
    parent.finish();

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(DB_CONNECTION_SPAN_COUNT + 3, spans.size());

    checkSameTrace(spans);
    checkNoEmptyTags(spans);
  }

  @Test
  public void spring_with_ignored_statement() throws Exception {
    BasicDataSource dataSource = getDataSource(false, Arrays.asList(
        "CREATE TABLE ignored (id INTEGER, TEST VARCHAR)",
        "INSERT INTO ignored (id, \\\"TEST\\\") VALUES (1, 'value')"
    ));

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("CREATE TABLE ignored (id INTEGER, TEST VARCHAR)");
    jdbcTemplate.execute("INSERT INTO ignored (id, \"TEST\") VALUES (1, 'value')");
    jdbcTemplate.execute("CREATE TABLE not_ignored (id INTEGER)");

    dataSource.close();

    List<MockSpan> finishedSpans = mockTracer.finishedSpans();
    assertEquals(DB_CONNECTION_SPAN_COUNT + 1, finishedSpans.size());
    checkNoEmptyTags(finishedSpans);
  }

  private BasicDataSource getDataSource(boolean traceWithActiveSpanOnly) {
    return getDataSource(traceWithActiveSpanOnly, new ArrayList<String>());
  }

  private BasicDataSource getDataSource(boolean traceWithActiveSpanOnly, List<String> ignored) {

    String ignoreForTracing = TestUtil.buildIgnoredString(ignored);

    BasicDataSource dataSource = new BasicDataSource();
    dataSource
        .setUrl("jdbc:tracing:h2:mem:spring?" + ignoreForTracing +
            "traceWithActiveSpanOnly=" + traceWithActiveSpanOnly);
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    return dataSource;
  }

}
