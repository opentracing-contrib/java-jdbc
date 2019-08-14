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

public class MysqlURLParser extends AbstractURLParser {

  private static final int DEFAULT_PORT = 3306;

  protected String dbType() {
    return "mysql";
  }

  @Override
  protected URLLocation fetchDatabaseHostsIndexRange(String url) {
    int hostLabelStartIndex = url.indexOf("//");
    int hostLabelEndIndex = url.indexOf("/", hostLabelStartIndex + 2);
    if (hostLabelEndIndex == -1) {
      hostLabelEndIndex = url.indexOf("?", hostLabelStartIndex + 2);
    }
    return new URLLocation(hostLabelStartIndex + 2, hostLabelEndIndex);
  }

  protected String fetchDatabaseNameFromURL(String url, int startSize) {
    URLLocation hostsLocation = fetchDatabaseNameIndexRange(url, startSize);
    if (hostsLocation == null) {
      return "";
    }
    return url.substring(hostsLocation.startIndex(), hostsLocation.endIndex());
  }

  protected URLLocation fetchDatabaseNameIndexRange(String url, int startSize) {
    int databaseStartTag = url.indexOf("/", startSize);
    if (databaseStartTag == -1) {
      return null;
    }
    int databaseEndTag = url.indexOf("?", databaseStartTag);
    if (databaseEndTag == -1) {
      databaseEndTag = url.length();
    }
    return new URLLocation(databaseStartTag + 1, databaseEndTag);
  }

  @Override
  protected URLLocation fetchDatabaseNameIndexRange(String url) {
    int databaseStartTag = url.lastIndexOf("/");
    int databaseEndTag = url.indexOf("?", databaseStartTag);
    if (databaseEndTag == -1) {
      databaseEndTag = url.length();
    }
    return new URLLocation(databaseStartTag + 1, databaseEndTag);
  }

  @Override
  public ConnectionInfo parse(String url) {
    URLLocation location = fetchDatabaseHostsIndexRange(url);
    String hosts = url.substring(location.startIndex(), location.endIndex());
    String[] hostSegment = hosts.split(",");
    if (hostSegment.length > 1) {
      StringBuilder sb = new StringBuilder();
      for (String host : hostSegment) {
        if (host.split(":").length == 1) {
          sb.append(host + ":" + DEFAULT_PORT + ",");
        } else {
          sb.append(host + ",");
        }
      }
      if (',' == sb.charAt(sb.length() - 1)) {
        sb.deleteCharAt(sb.length() - 1);
      }
      return new ConnectionInfo.Builder(sb.toString()).dbType(dbType())
          .dbInstance(fetchDatabaseNameFromURL(url)).build();
    } else {
      String[] hostAndPort = hostSegment[0].split(":");
      if (hostAndPort.length != 1) {
        return new ConnectionInfo.Builder(hostAndPort[0], Integer.valueOf(hostAndPort[1]))
            .dbType(dbType()).dbInstance(fetchDatabaseNameFromURL(url, location.endIndex()))
            .build();
      } else {

        return new ConnectionInfo.Builder(hostAndPort[0], DEFAULT_PORT).dbType(dbType())
            .dbInstance(fetchDatabaseNameFromURL(url, location.endIndex())).build();
      }
    }
  }

}
