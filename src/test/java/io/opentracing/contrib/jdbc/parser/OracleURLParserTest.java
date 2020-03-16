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
package io.opentracing.contrib.jdbc.parser;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentracing.contrib.jdbc.ConnectionInfo;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OracleURLParserTest {
  private static final String ORACLE = "oracle";
  private final OracleURLParser testee = new OracleURLParser();

  private static Stream<Arguments> easyConnectUrls() {
    return Stream.of( //
        Arguments
            .of("jdbc:oracle:thin:@192.168.105.100", "192.168.105.100:1521", "192.168.105.100"), //
        Arguments.of("jdbc:oracle:thin:@192.168.105.100:1234", "192.168.105.100:1234",
            "192.168.105.100"), //
        Arguments.of("jdbc:oracle:thin:@localhost/XEPDB1", "localhost:1521", "XEPDB1"), //
        Arguments.of("jdbc:oracle:thin:@localhost:1234/XEPDB1", "localhost:1234", "XEPDB1"), //
        Arguments.of("jdbc:oracle:thin:@//localhost:1234/XEPDB1", "localhost:1234", "XEPDB1"), //
        Arguments.of("jdbc:oracle:thin:@//localhost:1234/XEPDB1:server/instance", "localhost:1234",
            "XEPDB1"), //
        Arguments.of("jdbc:oracle:oci:@//localhost:1234/XEPDB1:server/instance", "localhost:1234",
            "XEPDB1") //
    );
  }

  private static Stream<Arguments> tnsNameUrls() {
    return Stream.of( //
        Arguments
            .of("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST= localhost )(PORT= 1521))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=orcl)))",
                "localhost:1521", "orcl"), //
        Arguments
            .of("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL= TCP)(HOST=hostA)(PORT= 1523 ))(ADDRESS=(PROTOCOL=TCP)(HOST=hostB)(PORT= 1521 )))(SOURCE_ROUTE=yes)(CONNECT_DATA=(SERVICE_NAME=orcl)))",
                "hostA:1523,hostB:1521", "orcl"), //
        Arguments
            .of("jdbc:oracle:oci:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL= TCP)(HOST=hostA)(PORT= 1523 ))(ADDRESS=(PROTOCOL=TCP)(HOST=hostB)(PORT= 1521 )))(SOURCE_ROUTE=yes)(CONNECT_DATA=(SERVICE_NAME=orcl)))",
                "hostA:1523,hostB:1521", "orcl") //
    );
  }

  private static Stream<Arguments> otherUrls() {
    return Stream.of( //
        Arguments.of("jdbc:oracle:thin:@localhost:orcl", "localhost:1521", "orcl"), //
        Arguments.of("jdbc:oracle:thin:@localhost:1522:orcl", "localhost:1522", "orcl"), //
        Arguments.of("jdbc:oracle:thin:@//localhost:1521/orcl", "localhost:1521", "orcl"), //
        Arguments.of("jdbc:oracle:thin:scott/tiger@myhost:1521:orcl", "myhost:1521", "orcl"), //
        Arguments.of("jdbc:oracle:thin:@orcl", "orcl:1521", "orcl"), //
        Arguments.of("jdbc:oracle:oci:@orcl", "orcl:1521", "orcl") //
    );
  }

  @ParameterizedTest(name = "[{0}]")
  @MethodSource("easyConnectUrls")
  void parseEasyConnect(final String url, final String dbPeer, final String dbInstance) {
    final ConnectionInfo result = testee.parse(url);
    assertThat(result.getDbType()).isEqualTo(ORACLE);
    assertThat(result.getDbPeer()).isEqualTo(dbPeer);
    assertThat(result.getDbInstance()).isEqualTo(dbInstance);
  }

  @ParameterizedTest(name = "[{0}]")
  @MethodSource("tnsNameUrls")
  void parseTnsName(final String url, final String dbPeer, final String dbInstance) {
    final ConnectionInfo result = testee.parse(url);
    assertThat(result.getDbType()).isEqualTo(ORACLE);
    assertThat(result.getDbPeer()).isEqualTo(dbPeer);
    assertThat(result.getDbInstance()).isEqualTo(dbInstance);
  }

  @ParameterizedTest(name = "[{0}]")
  @MethodSource("otherUrls")
  void parseOther(final String url, final String dbPeer, final String dbInstance) {
    final ConnectionInfo result = testee.parse(url);
    assertThat(result.getDbType()).isEqualTo(ORACLE);
    assertThat(result.getDbPeer()).isEqualTo(dbPeer);
    assertThat(result.getDbInstance()).isEqualTo(dbInstance);
  }
}