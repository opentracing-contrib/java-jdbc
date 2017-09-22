package io.opentracing.contrib.jdbc;

import static org.junit.Assert.assertEquals;

import io.opentracing.mock.MockSpan;
import java.util.List;

public class TestUtil {

  public static void checkSameTrace(List<MockSpan> spans) {
    for (int i = 0; i < spans.size() - 1; i++) {
      assertEquals(spans.get(i).context().traceId(), spans.get(i + 1).context().traceId());
      assertEquals(spans.get(spans.size() - 1).context().spanId(), spans.get(i).parentId());
    }
  }
}
