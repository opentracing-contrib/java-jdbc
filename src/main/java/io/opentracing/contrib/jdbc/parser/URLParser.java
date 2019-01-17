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

import java.util.LinkedHashMap;
import java.util.Map;

public class URLParser {
    private static final String MYSQL_JDBC_URL_PREFIX = "jdbc:mysql";
    private static final String ORACLE_JDBC_URL_PREFIX = "jdbc:oracle";
    private static final String H2_JDBC_URL_PREFIX = "jdbc:h2";
    private static final String POSTGRESQL_JDBC_URL_PREFIX = "jdbc:postgresql";
    private static final Map<String, ConnectionURLParser> parserRegister = new LinkedHashMap<String, ConnectionURLParser>();

    static {
        // put mysql parser firstly
        parserRegister.put(MYSQL_JDBC_URL_PREFIX, new MysqlURLParser());
        parserRegister.put(ORACLE_JDBC_URL_PREFIX, new OracleURLParser());
        parserRegister.put(H2_JDBC_URL_PREFIX, new H2URLParser());
        parserRegister.put(POSTGRESQL_JDBC_URL_PREFIX, new PostgreSQLURLParser());
    }

    /**
     * parse the url to the ConnectionInfo
     *
     * @param url
     * @return
     */
    public static ConnectionInfo parser(String url) {
        String lowerCaseUrl = url.toLowerCase();
        ConnectionURLParser parser = findURLParser(lowerCaseUrl);
        if (parser == null) {
            return ConnectionInfo.UNKNOWN_CONNECTION_INFO;
        }
        return parser.parse(lowerCaseUrl);
    }

    private static ConnectionURLParser findURLParser(String lowerCaseUrl) {
        for (Map.Entry<String, ConnectionURLParser> entry : parserRegister.entrySet()) {
            if (lowerCaseUrl.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Determine if urlPrefix is registered
     *
     * @param urlPrefix
     * @return
     */
    public static boolean isRegistered(String urlPrefix) {
        return parserRegister.containsKey(urlPrefix.toLowerCase());
    }

    /**
     * register new ConnectionURLParser.urlPrefix is required to be not registered
     *
     * @param urlPrefix
     * @param parser
     */
    public static void registerConnectionParser(String urlPrefix, ConnectionURLParser parser) {
        if (null == urlPrefix || parser == null) {
            throw new IllegalArgumentException("urlPrefix and parser can not be null");
        }
        String lowerCaseUrlPrefix = urlPrefix.toLowerCase();
        if (parserRegister.containsKey(lowerCaseUrlPrefix)) {
            throw new IllegalArgumentException(urlPrefix + " is already registered");
        }
        parserRegister.put(lowerCaseUrlPrefix, parser);
    }

    /**
     * register new ConnectionURLParser.When urlPrefix is registered,no exception occurs
     *
     * @param urlPrefix
     * @param parser
     */
    public static void safeRegisterConnectionParser(String urlPrefix, ConnectionURLParser parser) {
        if (isRegistered(urlPrefix)) {
            return;
        }
        registerConnectionParser(urlPrefix, parser);
    }
}
