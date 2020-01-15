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

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public class DynamicProxyTest {
  private interface A {
  }

  private interface B extends A {
  }

  private interface C {
  }

  private interface D extends B {
  }

  private class Bar implements C, D {
  }

  private class Foo extends Bar implements B {
  }

  @Test
  public void test() {
    final Foo foo = new Foo();
    final Class<?>[] ifaces = DynamicProxy.getAllInterfaces(foo.getClass());
    assertTrue(Arrays.stream(ifaces).anyMatch(i -> i == A.class));
    assertTrue(Arrays.stream(ifaces).anyMatch(i -> i == B.class));
    assertTrue(Arrays.stream(ifaces).anyMatch(i -> i == C.class));
    assertTrue(Arrays.stream(ifaces).anyMatch(i -> i == D.class));
  }
}