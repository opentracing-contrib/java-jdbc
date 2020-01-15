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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Seva Safris
 */
public class DynamicProxy {
  private static void recurse(final Class<?> iface, final Set<Class<?>> set) {
    if (set.contains(iface)) {
      return;
    }

    set.add(iface);
    for (final Class<?> extended : iface.getInterfaces()) {
      recurse(extended, set);
    }
  }

  static Class<?>[] getAllInterfaces(final Class<?> cls) {
    Class<?> next = cls;
    final Set<Class<?>> ifaces = new HashSet<>();
    do {
      for (final Class<?> iface : next.getInterfaces()) {
        recurse(iface, ifaces);
      }

      next = next.getSuperclass();
    }
    while (next != null);

    return ifaces.toArray(new Class[ifaces.size()]);
  }

  @SuppressWarnings("unchecked")
  public static <T> T wrap(final T target, final T wrapper) {
    final Class<?> cls = target.getClass();
    return (T) Proxy
        .newProxyInstance(cls.getClassLoader(), getAllInterfaces(cls), new InvocationHandler() {
          @Override
          public Object invoke(final Object proxy, final Method method, final Object[] args)
              throws Throwable {
            return method.invoke(wrapper, args);
          }
        });
  }
}