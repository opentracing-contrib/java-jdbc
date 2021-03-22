/*
 * Copyright 2017-2021 The OpenTracing Authors
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
package io.opentracing.contrib.jdbc.parser;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentracing.contrib.jdbc.ConnectionInfo;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SqlServerURLParserTest {
  private static final String SQLSERVER = "sqlserver";
  private final SqlServerURLParser urlParser = new SqlServerURLParser();

  private static Stream<Arguments> connectUrls() {
    return Stream.of(
        Arguments.of("jdbc:sqlserver://localhost\\instanceName:1435", "localhost:1435", null),
        Arguments.of("jdbc:sqlserver://localhost;integratedSecurity=true;", "localhost:1433", null),
        Arguments
            .of("jdbc:sqlserver://localhost;databaseName=AdventureWorks;integratedSecurity=true;",
                "localhost:1433", "AdventureWorks"),
        Arguments
            .of("jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;integratedSecurity=true;",
                "localhost:1433", "AdventureWorks"),
        Arguments
            .of("jdbc:sqlserver://localhost;databaseName=AdventureWorks;integratedSecurity=true;applicationName=MyApp;",
                "localhost:1433", "AdventureWorks")
    );
  }

  @ParameterizedTest(name = "[{0}]")
  @MethodSource("connectUrls")
  void parseConnectUrls(final String url, final String dbPeer, final String dbInstance) {
    final ConnectionInfo result = urlParser.parse(url);
    assertThat(result.getDbType()).isEqualTo(SQLSERVER);
    assertThat(result.getDbPeer()).isEqualTo(dbPeer);
    assertThat(result.getDbInstance()).isEqualTo(dbInstance);
  }
}