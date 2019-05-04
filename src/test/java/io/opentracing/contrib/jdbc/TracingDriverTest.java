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