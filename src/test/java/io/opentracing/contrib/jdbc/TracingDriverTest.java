package io.opentracing.contrib.jdbc;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import org.junit.Before;
import org.junit.Test;

public class TracingDriverTest {

  @Before
  public void before() {
    GlobalTracerTestUtil.resetGlobalTracer();
  }

  @Test
  public void testGlobalTracer() {
    TracingDriver tracingDriver = new TracingDriver();
    assertNotNull(tracingDriver.getTracer());
  }

  @Test
  public void testExplicitTracer() {
    Tracer tracer = new MockTracer();
    GlobalTracer.registerIfAbsent(tracer);
    Tracer tracer2 = new MockTracer();
    TracingDriver tracingDriver = new TracingDriver();
    tracingDriver.setTracer(tracer2);
    assertEquals(tracer2, tracingDriver.getTracer());
  }

}