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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import io.opentracing.mock.MockSpan;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

class TestUtil {

  static void checkSameTrace(List<MockSpan> spans) {
    for (int i = 0; i < spans.size() - 1; i++) {
      assertEquals(spans.get(i).context().traceId(), spans.get(i + 1).context().traceId());
      assertEquals(spans.get(spans.size() - 1).context().spanId(), spans.get(i).parentId());
    }
  }

  static void checkNoEmptyTags(List<MockSpan> spans) {
    for (MockSpan span : spans) {
      for (Entry<String, Object> entry : span.tags().entrySet()) {
        assertNotNull(entry.getValue());
        if (entry.getValue() instanceof String) {
          String tagValue = (String) entry.getValue();
          assertFalse(tagValue.trim().isEmpty());
        }
      }
    }
  }

  static String buildIgnoredString(Collection<String> ignored) {
    StringBuilder ignoreForTracing = new StringBuilder();
    for (String query : ignored) {
      ignoreForTracing.append("ignoreForTracing=\"");
      ignoreForTracing.append(query.replaceAll("\"", "\\\""));
      ignoreForTracing.append("\";");
    }
    return ignoreForTracing.toString();
  }
}
