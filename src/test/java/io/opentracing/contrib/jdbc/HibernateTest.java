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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.opentracing.Scope;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracerTestUtil;
import io.opentracing.util.ThreadLocalScopeManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Persistence;
import javax.persistence.Table;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HibernateTest {

  private static final MockTracer mockTracer = new MockTracer(new ThreadLocalScopeManager(),
      MockTracer.Propagator.TEXT_MAP);

  @BeforeClass
  public static void init() {
    GlobalTracerTestUtil.setGlobalTracerUnconditionally(mockTracer);
  }

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void jpa() {
    EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("jpa");

    Employee employee = new Employee();
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.getTransaction().begin();
    entityManager.persist(employee);
    entityManager.getTransaction().commit();
    entityManager.close();
    entityManagerFactory.close();

    assertNotNull(employee.id);

    List<MockSpan> finishedSpans = mockTracer.finishedSpans();
    assertEquals(9, finishedSpans.size());

    checkSpans(finishedSpans, "jpa");
    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void jpa_with_active_span_only() {
    EntityManagerFactory entityManagerFactory = Persistence
        .createEntityManagerFactory("jpa_active_span_only");

    Employee employee = new Employee();
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.getTransaction().begin();
    entityManager.persist(employee);
    entityManager.getTransaction().commit();
    entityManager.close();
    entityManagerFactory.close();

    assertNotNull(employee.id);

    List<MockSpan> finishedSpans = mockTracer.finishedSpans();
    assertEquals(0, finishedSpans.size());

    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void jpa_with_parent() {
    final MockSpan parent = mockTracer.buildSpan("parent").start();
    try (Scope ignored = mockTracer.activateSpan(parent)) {
      EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("jpa");

      EntityManager entityManager = entityManagerFactory.createEntityManager();

      entityManager.getTransaction().begin();
      entityManager.persist(new Employee());
      entityManager.persist(new Employee());
      entityManager.getTransaction().commit();
      entityManager.close();
      entityManagerFactory.close();
    }
    parent.finish();

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(12, spans.size());
    checkSameTrace(spans);
    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void jpa_with_parent_and_active_span_only() {
    final MockSpan parent = mockTracer.buildSpan("parent").start();
    try (Scope ignored = mockTracer.activateSpan(parent)) {
      EntityManagerFactory entityManagerFactory = Persistence
          .createEntityManagerFactory("jpa_active_span_only");

      EntityManager entityManager = entityManagerFactory.createEntityManager();

      entityManager.getTransaction().begin();
      entityManager.persist(new Employee());
      entityManager.persist(new Employee());
      entityManager.getTransaction().commit();
      entityManager.close();
      entityManagerFactory.close();
    }
    parent.finish();

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(12, spans.size());
    checkSameTrace(spans);
    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void hibernate() {
    SessionFactory sessionFactory = createSessionFactory(false);
    Session session = sessionFactory.openSession();

    Employee employee = new Employee();
    session.beginTransaction();
    session.save(employee);
    session.getTransaction().commit();
    session.close();
    sessionFactory.close();

    assertNotNull(employee.id);

    List<MockSpan> finishedSpans = mockTracer.finishedSpans();
    assertEquals(9, finishedSpans.size());

    checkSpans(finishedSpans, "hibernate");
    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void hibernate_with_active_span_only() {
    SessionFactory sessionFactory = createSessionFactory(true);
    Session session = sessionFactory.openSession();

    Employee employee = new Employee();
    session.beginTransaction();
    session.save(employee);
    session.getTransaction().commit();
    session.close();
    sessionFactory.close();

    assertNotNull(employee.id);

    List<MockSpan> finishedSpans = mockTracer.finishedSpans();
    assertEquals(0, finishedSpans.size());

    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void hibernate_with_parent() {
    final MockSpan parent = mockTracer.buildSpan("parent").start();
    try (Scope ignored = mockTracer.activateSpan(parent)) {
      SessionFactory sessionFactory = createSessionFactory(false);
      Session session = sessionFactory.openSession();

      session.beginTransaction();
      session.save(new Employee());
      session.save(new Employee());
      session.getTransaction().commit();
      session.close();
      sessionFactory.close();
    }
    parent.finish();

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(12, spans.size());
    checkSameTrace(spans);
    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void hibernate_with_parent_and_active_span_only() {
    final MockSpan parent = mockTracer.buildSpan("parent").start();
    try (Scope ignored = mockTracer.activateSpan(parent)) {
      SessionFactory sessionFactory = createSessionFactory(true);
      Session session = sessionFactory.openSession();

      session.beginTransaction();
      session.save(new Employee());
      session.save(new Employee());
      session.getTransaction().commit();
      session.close();
      sessionFactory.close();
    }
    parent.finish();

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(12, spans.size());
    checkSameTrace(spans);
    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void hibernate_with_ignored_statement() {
    SessionFactory sessionFactory = createSessionFactory(false,
        Collections.singletonList("insert into Employee (id) values (?)"));
    Session session = sessionFactory.openSession();

    Employee employee = new Employee();
    session.beginTransaction();
    session.save(employee);
    session.getTransaction().commit();
    session.close();
    sessionFactory.close();

    assertNotNull(employee.id);

    List<MockSpan> finishedSpans = mockTracer.finishedSpans();
    assertEquals(8, finishedSpans.size());

    checkSpans(finishedSpans, "hibernate");
    assertNull(mockTracer.activeSpan());
  }

  private void checkSpans(List<MockSpan> mockSpans, String dbInstance) {
    checkNoEmptyTags(mockSpans);
    for (MockSpan mockSpan : mockSpans) {
      assertEquals(Tags.SPAN_KIND_CLIENT, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
      assertEquals(JdbcTracingUtils.COMPONENT_NAME, mockSpan.tags().get(Tags.COMPONENT.getKey()));
      assertEquals("h2", mockSpan.tags().get(Tags.DB_TYPE.getKey()));

      assertEquals(dbInstance, mockSpan.tags().get(Tags.DB_INSTANCE.getKey()));
      assertEquals("localhost:-1", mockSpan.tags().get("peer.address"));

      final String sql = (String) mockSpan.tags().get(Tags.DB_STATEMENT.getKey());
      if (sql != null) {
        // empty sql should not be added to avoid NPE in tracers
        assertFalse(sql.trim().isEmpty());
      }
      assertEquals(0, mockSpan.generatedErrors().size());
    }
  }

  private SessionFactory createSessionFactory(boolean traceWithActiveSpanOnly) {
    return createSessionFactory(traceWithActiveSpanOnly, new ArrayList<String>());
  }

  private SessionFactory createSessionFactory(boolean traceWithActiveSpanOnly,
      List<String> ignored) {
    String ignoredForTrace = TestUtil.buildIgnoredString(ignored);
    Configuration configuration = new Configuration();
    configuration.addAnnotatedClass(Employee.class);
    configuration.setProperty("hibernate.connection.driver_class",
        "io.opentracing.contrib.jdbc.TracingDriver");
    configuration.setProperty("hibernate.connection.url",
        "jdbc:tracing:h2:mem:hibernate?" + ignoredForTrace + "traceWithActiveSpanOnly="
            + traceWithActiveSpanOnly);
    configuration.setProperty("hibernate.connection.username", "sa");
    configuration.setProperty("hibernate.connection.password", "");
    configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
    configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
    configuration.setProperty("hibernate.show_sql", "true");
    configuration.setProperty("hibernate.connection.pool_size", "10");

    StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
        .applySettings(configuration.getProperties());
    return configuration.buildSessionFactory(builder.build());
  }


  @Entity
  @Table(name = "Employee")
  private static class Employee {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
  }
}
