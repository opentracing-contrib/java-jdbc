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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import org.junit.Test;

@SuppressWarnings("all")
public class DynamicProxyTest {
  private static final Comparator<Class<?>> comparator = new Comparator<Class<?>>() {
    @Override
    public int compare(final Class<?> o1, final Class<?> o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

  private static void testGetAllInterfaces(final Object obj, final Class<?> ... expecteds) {
    final Class<?>[] ifaces = DynamicProxy.getAllInterfaces(obj.getClass());
    Arrays.sort(ifaces, comparator);
    Arrays.sort(expecteds, comparator);
    assertEquals(Arrays.toString(ifaces), Arrays.toString(expecteds));
    for (final Class<?> iface : ifaces)
      assertTrue(Arrays.stream(ifaces).anyMatch(i -> i == iface));
  }

  private interface A {
  }

  private interface B extends A {
  }

  private interface C extends B {
  }

  private interface D {
  }

  private class Bar implements C, D {
  }

  private class Foo extends Bar implements B {
  }

  @Test
  public void testA() {
    testGetAllInterfaces(new A() {}, A.class);
  }

  @Test
  public void testB() {
    testGetAllInterfaces(new B() {}, A.class, B.class);
  }

  @Test
  public void testC() {
    testGetAllInterfaces(new C() {}, A.class, B.class, C.class);
  }

  @Test
  public void testD() {
    testGetAllInterfaces(new D() {}, D.class);
  }

  @Test
  public void testFoo() {
    testGetAllInterfaces(new Foo(), A.class, B.class, C.class, D.class);
  }

  @Test
  public void testBar() {
    testGetAllInterfaces(new Bar(), A.class, B.class, C.class, D.class);
  }
}