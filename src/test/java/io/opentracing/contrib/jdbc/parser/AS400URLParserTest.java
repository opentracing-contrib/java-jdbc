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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import io.opentracing.contrib.jdbc.ConnectionInfo;
import org.junit.Test;

/**
 * Test for {@link AS400URLParser}
 *
 * @author Capgemini
 */
public class AS400URLParserTest {

  @Test
  public void testMatchShort() {
    String url = "jdbc:as400://myhost";
    ConnectionInfo connectionInfo = URLParser.parse(url);
    assertThat(connectionInfo, is(not(equalTo(ConnectionInfo.UNKNOWN_CONNECTION_INFO))));
    assertThat(connectionInfo.getDbPeer(), is(equalTo("myhost")));
    assertThat(connectionInfo.getDbInstance(),
        is(equalTo(ConnectionInfo.UNKNOWN_CONNECTION_INFO.getDbInstance())));
    assertThat(connectionInfo.getDbType(), is(equalTo("as400")));
  }

  @Test
  public void testMatchShortWithSlash() {
    String url = "jdbc:as400://myhost/";
    ConnectionInfo connectionInfo = URLParser.parse(url);
    assertThat(connectionInfo, is(not(equalTo(ConnectionInfo.UNKNOWN_CONNECTION_INFO))));
    assertThat(connectionInfo.getDbPeer(), is(equalTo("myhost")));
    assertThat(connectionInfo.getDbInstance(),
        is(equalTo(ConnectionInfo.UNKNOWN_CONNECTION_INFO.getDbInstance())));
    assertThat(connectionInfo.getDbType(), is(equalTo("as400")));
  }

  @Test
  public void testMatchShortWithSlashAnsOptions() {
    String url = "jdbc:as400://myhost/;naming=sql;errors=full";
    ConnectionInfo connectionInfo = URLParser.parse(url);
    assertThat(connectionInfo, is(not(equalTo(ConnectionInfo.UNKNOWN_CONNECTION_INFO))));
    assertThat(connectionInfo.getDbPeer(), is(equalTo("myhost")));
    assertThat(connectionInfo.getDbInstance(),
        is(equalTo(ConnectionInfo.UNKNOWN_CONNECTION_INFO.getDbInstance())));
    assertThat(connectionInfo.getDbType(), is(equalTo("as400")));
  }


  @Test
  public void testWithOptions() {
    String url = "jdbc:as400://myhost.mydomain.com;naming=sql;errors=full";
    ConnectionInfo connectionInfo = URLParser.parse(url);
    assertThat(connectionInfo, is(not(equalTo(ConnectionInfo.UNKNOWN_CONNECTION_INFO))));
    assertThat(connectionInfo.getDbPeer(), is(equalTo("myhost.mydomain.com")));
    assertThat(connectionInfo.getDbInstance(),
        is(equalTo(ConnectionInfo.UNKNOWN_CONNECTION_INFO.getDbInstance())));
    assertThat(connectionInfo.getDbType(), is(equalTo("as400")));
  }

  @Test
  public void testFull() {
    String url = "jdbc:as400://myhost.mydomain.com/myinstance;naming=sql;errors=full";
    ConnectionInfo connectionInfo = URLParser.parse(url);
    assertThat(connectionInfo, is(not(equalTo(ConnectionInfo.UNKNOWN_CONNECTION_INFO))));
    assertThat(connectionInfo.getDbPeer(), is(equalTo("myhost.mydomain.com")));
    assertThat(connectionInfo.getDbInstance(), is(equalTo("myinstance")));
    assertThat(connectionInfo.getDbType(), is(equalTo("as400")));
  }

  @Test
  public void testFullWithSlash() {
    String url = "jdbc:as400://myhost.mydomain.com/myinstance/;naming=sql;errors=full";
    ConnectionInfo connectionInfo = URLParser.parse(url);
    assertThat(connectionInfo, is(not(equalTo(ConnectionInfo.UNKNOWN_CONNECTION_INFO))));
    assertThat(connectionInfo.getDbPeer(), is(equalTo("myhost.mydomain.com")));
    assertThat(connectionInfo.getDbInstance(), is(equalTo("myinstance")));
    assertThat(connectionInfo.getDbType(), is(equalTo("as400")));
  }


  @Test
  public void testNotMatch() {
    String url = "jdbc:as400:notmatching";
    ConnectionInfo connectionInfo = URLParser.parse(url);
    assertThat(connectionInfo, is(equalTo(ConnectionInfo.UNKNOWN_CONNECTION_INFO)));
  }

}
