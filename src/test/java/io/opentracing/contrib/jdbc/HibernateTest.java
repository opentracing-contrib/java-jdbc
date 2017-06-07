package io.opentracing.contrib.jdbc;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
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
    assertEquals(8, finishedSpans.size());

    checkSpans(finishedSpans);
    assertNull(mockTracer.activeSpan());
  }

  @Test
  public void hibernate() throws InterruptedException {
    SessionFactory sessionFactory = createSessionFactory();
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

    checkSpans(finishedSpans);
    assertNull(mockTracer.activeSpan());
  }

  private void checkSpans(List<MockSpan> mockSpans) {
    for (MockSpan mockSpan : mockSpans) {
      assertEquals(Tags.SPAN_KIND_CLIENT, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
      assertEquals(JdbcTracingUtils.COMPONENT_NAME, mockSpan.tags().get(Tags.COMPONENT.getKey()));
      assertEquals("h2", mockSpan.tags().get(Tags.DB_TYPE.getKey()));
      assertEquals("sa", mockSpan.tags().get(Tags.DB_USER.getKey()));
      assertNotNull(mockSpan.tags().get(Tags.DB_STATEMENT.getKey()));
      assertEquals(0, mockSpan.generatedErrors().size());
    }
  }

  private SessionFactory createSessionFactory() {
    Configuration configuration = new Configuration();
    configuration.addAnnotatedClass(Employee.class);
    configuration.setProperty("hibernate.connection.driver_class",
        "io.opentracing.contrib.jdbc.TracingDriver");
    configuration.setProperty("hibernate.connection.url", "jdbc:tracing:h2:mem:hibernate");
    configuration.setProperty("hibernate.connection.username", "sa");
    configuration.setProperty("hibernate.connection.password", "");
    configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
    configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
    configuration.setProperty("hibernate.show_sql", "true");
    configuration.setProperty("hibernate.connection.pool_size", "10");

    StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
        .applySettings(configuration.getProperties());
    SessionFactory sessionFactory = configuration.buildSessionFactory(builder.build());
    return sessionFactory;
  }


  @Entity
  @Table(name = "Employee")
  public static class Employee {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
  }
}
