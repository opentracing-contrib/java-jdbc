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
import java.util.ArrayList;
import java.util.List;

public class OracleURLParser extends AbstractURLParser {

  public static final String SERVICE_NAME_FLAG = "@//";
  public static final String TNSNAME_URL_FLAG = "DESCRIPTION";
  private static final String DB_TYPE = "oracle";
  private static final int DEFAULT_PORT = 1521;

  @Override
  protected URLLocation fetchDatabaseHostsIndexRange(String url) {
    int hostLabelStartIndex;
    if (isServiceNameURL(url)) {
      hostLabelStartIndex = url.indexOf(SERVICE_NAME_FLAG) + 3;
    } else {
      hostLabelStartIndex = url.indexOf("@") + 1;
    }
    int hostLabelEndIndex = url.lastIndexOf(":");
    return new URLLocation(hostLabelStartIndex, hostLabelEndIndex);
  }

  @Override
  protected URLLocation fetchDatabaseNameIndexRange(String url) {
    int hostLabelStartIndex;
    int hostLabelEndIndex = url.length();
    if (isServiceNameURL(url)) {
      hostLabelStartIndex = url.lastIndexOf("/") + 1;
    } else if (isTNSNameURL(url)) {
      hostLabelStartIndex = url.indexOf("=", url.indexOf("SERVICE_NAME")) + 1;
      hostLabelEndIndex = url.indexOf(")", hostLabelStartIndex);
    } else {
      hostLabelStartIndex = url.lastIndexOf(":") + 1;
    }
    return new URLLocation(hostLabelStartIndex, hostLabelEndIndex);
  }

  private boolean isServiceNameURL(String url) {
    return url.contains(SERVICE_NAME_FLAG);
  }

  private boolean isTNSNameURL(String url) {
    return url.contains(TNSNAME_URL_FLAG);
  }

  @Override
  public ConnectionInfo parse(String url) {
    if (isTNSNameURL(url)) {
      return tnsNameURLParse(url);
    } else {
      return commonsURLParse(url);
    }
  }

  private ConnectionInfo commonsURLParse(final String url) {
    String host = fetchDatabaseHostsFromURL(url);
    String[] hostSegment = splitDatabaseAddress(host);
    String databaseName = fetchDatabaseNameFromURL(url);
    if (hostSegment.length == 1) {
      return new ConnectionInfo.Builder(host, DEFAULT_PORT).dbType(DB_TYPE).dbInstance(databaseName)
          .build();
    } else {
      return new ConnectionInfo.Builder(hostSegment[0], Integer.valueOf(hostSegment[1]))
          .dbType(DB_TYPE).dbInstance(databaseName).build();
    }
  }

  private ConnectionInfo tnsNameURLParse(final String url) {
    String host = parseDatabaseHostsFromURL(url);
    String databaseName = fetchDatabaseNameFromURL(url);
    return new ConnectionInfo.Builder(host).dbType(DB_TYPE).dbInstance(databaseName).build();
  }

  private String parseDatabaseHostsFromURL(String url) {
    int beginIndex = url.indexOf("DESCRIPTION");
    List<String> hosts = new ArrayList<String>();
    do {
      int hostStartIndex = url.indexOf("HOST", beginIndex);
      if (hostStartIndex == -1) {
        break;
      }
      int equalStartIndex = url.indexOf("=", hostStartIndex);
      int hostEndIndex = url.indexOf(")", hostStartIndex);
      String host = url.substring(equalStartIndex + 1, hostEndIndex);

      int port = DEFAULT_PORT;
      int portStartIndex = url.indexOf("PORT", hostEndIndex);
      int portEndIndex = url.length();
      if (portStartIndex != -1) {
        int portEqualStartIndex = url.indexOf("=", portStartIndex);
        portEndIndex = url.indexOf(")", portEqualStartIndex);
        port = Integer.parseInt(url.substring(portEqualStartIndex + 1, portEndIndex).trim());
      }
      hosts.add(host.trim() + ":" + port);
      beginIndex = portEndIndex;
    }
    while (true);
    return join(",", hosts);
  }

  private String join(String delimiter, List<String> list) {
    if (list == null || list.isEmpty()) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    for (int i = 0, len = list.size(); i < len; i++) {
      if (i == (len - 1)) {
        builder.append(list.get(i));
      } else {
        builder.append(list.get(i)).append(delimiter);
      }
    }
    return builder.toString();
  }

  private String[] splitDatabaseAddress(String address) {
    String[] hostSegment = address.split(":");
    return hostSegment;
  }
}
