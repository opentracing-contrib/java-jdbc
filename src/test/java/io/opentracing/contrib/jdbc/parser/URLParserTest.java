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
package io.opentracing.contrib.jdbc.parser;

import io.opentracing.contrib.jdbc.ConnectionInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class URLParserTest {

    private static final String MYSQL = "mysql";
    private static final String ORACLE = "oracle";
    private static final String POSTGRESQL = "postgresql";
    private static final String H2 = "h2";

    @Test
    public void testParseMysqlJDBCURLWithHost() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:mysql//primaryhost/test");
        assertEquals(MYSQL, connectionInfo.getDbType());
        assertEquals("test", connectionInfo.getDbInstance());
        assertEquals("primaryhost:3306", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseMysqlJDBCURLWithoutDB() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:mysql//primaryhost?profileSQL=true");
        assertEquals(MYSQL, connectionInfo.getDbType());
        assertEquals("", connectionInfo.getDbInstance());
        assertEquals("primaryhost:3306", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseMysqlJDBCURLWithHostAndPort() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:mysql//primaryhost:3307/test?profileSQL=true");
        assertEquals(MYSQL, connectionInfo.getDbType());
        assertEquals("test", connectionInfo.getDbInstance());
        assertEquals("primaryhost:3307", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseMysqlJDBCURLWithMultiHost() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:mysql//primaryhost:3307,secondaryhost1,secondaryhost2/test?profileSQL=true");
        assertEquals(MYSQL, connectionInfo.getDbType());
        assertEquals("test", connectionInfo.getDbInstance());
        assertEquals("primaryhost:3307,secondaryhost1:3306,secondaryhost2:3306,", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseMysqlJDBCURLWithConnectorJs() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:mysql:replication://master,slave1,slave2,slave3/test");
        assertEquals(MYSQL, connectionInfo.getDbType());
        assertEquals("test", connectionInfo.getDbInstance());
        assertEquals("master:3306,slave1:3306,slave2:3306,slave3:3306,", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseOracleJDBCURLWithHost() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:oracle:thin:@localhost:orcl");
        assertEquals(ORACLE, connectionInfo.getDbType());
        assertEquals("orcl", connectionInfo.getDbInstance());
        assertEquals("localhost:1521", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseOracleJDBCURLWithHostAndPort() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:oracle:thin:@localhost:1522:orcl");
        assertEquals(ORACLE, connectionInfo.getDbType());
        assertEquals("orcl", connectionInfo.getDbInstance());
        assertEquals("localhost:1522", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseOracleServiceName() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:oracle:thin:@//localhost:1521/orcl");
        assertEquals(ORACLE, connectionInfo.getDbType());
        assertEquals("orcl", connectionInfo.getDbInstance());
        assertEquals("localhost:1521", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseOracleTNSName() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST= localhost )(PORT= 1521))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=orcl)))");
        assertEquals(ORACLE, connectionInfo.getDbType());
        assertEquals("orcl", connectionInfo.getDbInstance());
        assertEquals("localhost:1521", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseOracleTNSNameWithMultiAddress() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL= TCP)(HOST=hostA)(PORT= 1523 ))(ADDRESS=(PROTOCOL=TCP)(HOST=hostB)(PORT= 1521 )))(SOURCE_ROUTE=yes)(CONNECT_DATA=(SERVICE_NAME=orcl)))");
        assertEquals(ORACLE, connectionInfo.getDbType());
        assertEquals("orcl", connectionInfo.getDbInstance());
        assertEquals("hostA:1523,hostB:1521", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseOracleJDBCURLWithUserNameAndPassword() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:oracle:thin:scott/tiger@myhost:1521:orcl");
        assertEquals(ORACLE, connectionInfo.getDbType());
        assertEquals("orcl", connectionInfo.getDbInstance());
        assertEquals("myhost:1521", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseH2JDBCURLWithEmbedded() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:h2:file:/data/sample");
        assertEquals(H2, connectionInfo.getDbType());
        assertEquals("/data/sample", connectionInfo.getDbInstance());
        assertEquals("localhost:-1", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseH2JDBCURLWithEmbeddedRunningInWindows() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:h2:file:c:/data/sample");
        assertEquals(H2, connectionInfo.getDbType());
        assertEquals("c:/data/sample", connectionInfo.getDbInstance());
        assertEquals("localhost:-1", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseH2JDBCURLWithMemoryMode() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:h2:mem:test_mem");

        assertEquals(H2, connectionInfo.getDbType());
        assertEquals("test_mem", connectionInfo.getDbInstance());
        assertEquals("localhost:-1", connectionInfo.getDbPeer());
    }

    @Test
    public void testParseH2JDBCURL() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:h2:tcp://localhost:8084/~/sample");
        assertEquals(H2, connectionInfo.getDbType());
        assertEquals("sample", connectionInfo.getDbInstance());
        assertEquals("localhost:8084", connectionInfo.getDbPeer());
    }

    @Test
    public void testParsePostgresqlJDBCURLWithHost() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:postgresql//primaryhost/test");
        assertEquals(POSTGRESQL, connectionInfo.getDbType());
        assertEquals("test", connectionInfo.getDbInstance());
        assertEquals("primaryhost:5432", connectionInfo.getDbPeer());
    }

    @Test
    public void testParsePostgresqlJDBCURLWithoutDB() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:postgresql//primaryhost?profileSQL=true");
        assertEquals(POSTGRESQL, connectionInfo.getDbType());
        assertEquals("", connectionInfo.getDbInstance());
        assertEquals("primaryhost:3306", connectionInfo.getDbPeer());
    }

    @Test
    public void testParsePostgresqlJDBCURLWithHostAndPort() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:postgresql//primaryhost:3307/test?profileSQL=true");
        assertEquals(POSTGRESQL, connectionInfo.getDbType());
        assertEquals("test", connectionInfo.getDbInstance());
        assertEquals("primaryhost:3307", connectionInfo.getDbPeer());
    }

    @Test
    public void testParsePostgresqlJDBCURLWithMultiHost() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:postgresql//primaryhost:3307,secondaryhost1,secondaryhost2/test?profileSQL=true");
        assertEquals(POSTGRESQL, connectionInfo.getDbType());
        assertEquals("test", connectionInfo.getDbInstance());
        assertEquals("primaryhost:3307,secondaryhost1:5432,secondaryhost2:5432,", connectionInfo.getDbPeer());
    }

    @Test
    public void testParsePostgresqlJDBCURLWithConnectorJs() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:postgresql:replication://master,slave1,slave2,slave3/test");
        assertEquals(POSTGRESQL, connectionInfo.getDbType());
        assertEquals("test", connectionInfo.getDbInstance());
        assertEquals("master:5432,slave1:5432,slave2:5432,slave3:5432,", connectionInfo.getDbPeer());
    }

    @Test
    public void testNoParserFound() {
        ConnectionInfo connectionInfo = URLParser.parser("jdbc:unkown_type//primaryhost?profileSQL=true");
        assertEquals(ConnectionInfo.UNKNOWN_CONNECTION_INFO, connectionInfo);
    }
}