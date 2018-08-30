/*
 * Copyright 2017-2018 The OpenTracing Authors
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


import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TracingDriver implements Driver {

  private static final Driver INSTANCE = new TracingDriver();

  private static final String TRACE_WITH_ACTIVE_SPAN_ONLY = "traceWithActiveSpanOnly";

  private static final String WITH_ACTIVE_SPAN_ONLY = TRACE_WITH_ACTIVE_SPAN_ONLY + "=true";
  public static final String IGNORE_FOR_TRACING_REGEX = "ignoreForTracing=\"((?:\\\\\"|[^\"])*)\"[;]*";

  public static final String MYSQL_DRIVER      = "com.mysql.jdbc.Driver";

  public static final String LOG4JDBC_DRIVER   = "net.sf.log4jdbc.DriverSpy";

  public static final String MARIADB_DRIVER    = "org.mariadb.jdbc.Driver";

  public static final String ORACLE_DRIVER     = "oracle.jdbc.driver.OracleDriver";

  public static final String ALI_ORACLE_DRIVER = "com.alibaba.jdbc.AlibabaDriver";

  public static final String DB2_DRIVER        = "COM.ibm.db2.jdbc.app.DB2Driver";

  public static final String H2_DRIVER         = "org.h2.Driver";

  public static final String DM_DRIVER         = "dm.jdbc.driver.DmDriver";

  public static final String KINGBASE_DRIVER   = "com.kingbase.Driver";


  static {
    try {
      DriverManager.registerDriver(INSTANCE);
    } catch (SQLException e) {
      throw new IllegalStateException("Could not register TracingDriver with DriverManager", e);
    }
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    // if there is no url, we have problems
    if (url == null) {
      throw new SQLException("url is required");
    }

    if (!acceptsURL(url)) {
      return null;
    }

    String realUrl = extractRealUrl(url);
    String dbType = extractDbType(realUrl);
    String dbUser = info.getProperty("user");
    
    Driver wrappedDriver = findTheDriver(realUrl);

    Connection connection = wrappedDriver.connect(realUrl, info);

    return new TracingConnection(connection, dbType, dbUser, url.contains(WITH_ACTIVE_SPAN_ONLY),
            extractIgnoredStatements(url));
  }

  /**
   *
   * @param realUrl
   * @return
   * @throws SQLException
     */
  public Driver findTheDriver(String realUrl) throws SQLException {
    // find the real driver for the URL
    try {
      Driver wrappedDriver = findDriver(realUrl);
      return wrappedDriver;
    } catch (SQLException e) {
      String wrappedDriverClassName = getDriverClassName(realUrl);

      Driver wrappedDriver = createDriver(null, wrappedDriverClassName);

      return wrappedDriver;
    }
  }

  public static String getDriverClassName(String rawUrl) throws SQLException {
    if (rawUrl.startsWith("jdbc:derby:")) {
      return "org.apache.derby.jdbc.EmbeddedDriver";
    } else if (rawUrl.startsWith("jdbc:mysql:")) {
      return MYSQL_DRIVER;
    } else if (rawUrl.startsWith("jdbc:log4jdbc:")) {
      return LOG4JDBC_DRIVER;
    } else if (rawUrl.startsWith("jdbc:mariadb:")) {
      return MARIADB_DRIVER;
    } else if (rawUrl.startsWith("jdbc:oracle:") //
            || rawUrl.startsWith("JDBC:oracle:")) {
      return ORACLE_DRIVER;
    } else if (rawUrl.startsWith("jdbc:alibaba:oracle:")) {
      return ALI_ORACLE_DRIVER;
    } else if (rawUrl.startsWith("jdbc:microsoft:")) {
      return "com.microsoft.jdbc.sqlserver.SQLServerDriver";
    } else if (rawUrl.startsWith("jdbc:sqlserver:")) {
      return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    } else if (rawUrl.startsWith("jdbc:sybase:Tds:")) {
      return "com.sybase.jdbc2.jdbc.SybDriver";
    } else if (rawUrl.startsWith("jdbc:jtds:")) {
      return "net.sourceforge.jtds.jdbc.Driver";
    } else if (rawUrl.startsWith("jdbc:fake:") || rawUrl.startsWith("jdbc:mock:")) {
      return "com.alibaba.druid.mock.MockDriver";
    } else if (rawUrl.startsWith("jdbc:postgresql:")) {
      return "org.postgresql.Driver";
    } else if (rawUrl.startsWith("jdbc:hsqldb:")) {
      return "org.hsqldb.jdbcDriver";
    } else if (rawUrl.startsWith("jdbc:db2:")) {
      return DB2_DRIVER;
    } else if (rawUrl.startsWith("jdbc:sqlite:")) {
      return "org.sqlite.JDBC";
    } else if (rawUrl.startsWith("jdbc:ingres:")) {
      return "com.ingres.jdbc.IngresDriver";
    } else if (rawUrl.startsWith("jdbc:h2:")) {
      return H2_DRIVER;
    } else if (rawUrl.startsWith("jdbc:mckoi:")) {
      return "com.mckoi.JDBCDriver";
    } else if (rawUrl.startsWith("jdbc:cloudscape:")) {
      return "COM.cloudscape.core.JDBCDriver";
    } else if (rawUrl.startsWith("jdbc:informix-sqli:")) {
      return "com.informix.jdbc.IfxDriver";
    } else if (rawUrl.startsWith("jdbc:timesten:")) {
      return "com.timesten.jdbc.TimesTenDriver";
    } else if (rawUrl.startsWith("jdbc:as400:")) {
      return "com.ibm.as400.access.AS400JDBCDriver";
    } else if (rawUrl.startsWith("jdbc:sapdb:")) {
      return "com.sap.dbtech.jdbc.DriverSapDB";
    } else if (rawUrl.startsWith("jdbc:JSQLConnect:")) {
      return "com.jnetdirect.jsql.JSQLDriver";
    } else if (rawUrl.startsWith("jdbc:JTurbo:")) {
      return "com.newatlanta.jturbo.driver.Driver";
    } else if (rawUrl.startsWith("jdbc:firebirdsql:")) {
      return "org.firebirdsql.jdbc.FBDriver";
    } else if (rawUrl.startsWith("jdbc:interbase:")) {
      return "interbase.interclient.Driver";
    } else if (rawUrl.startsWith("jdbc:pointbase:")) {
      return "com.pointbase.jdbc.jdbcUniversalDriver";
    } else if (rawUrl.startsWith("jdbc:edbc:")) {
      return "ca.edbc.jdbc.EdbcDriver";
    } else if (rawUrl.startsWith("jdbc:mimer:multi1:")) {
      return "com.mimer.jdbc.Driver";
    } else if (rawUrl.startsWith("jdbc:dm:")) {
      return DM_DRIVER;
    } else if (rawUrl.startsWith("jdbc:kingbase:")) {
      return KINGBASE_DRIVER;
    } else {
      throw new SQLException("unkow jdbc driver : " + rawUrl);
    }
  }

  @Override
  public boolean acceptsURL(String url) throws SQLException {
    return url != null && url.startsWith("jdbc:tracing:");
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
    return findDriver(url).getPropertyInfo(url, info);
  }

  @Override
  public int getMajorVersion() {
    // There is no way to get it from wrapped driver
    return 1;
  }

  @Override
  public int getMinorVersion() {
    // There is no way to get it from wrapped driver
    return 0;
  }

  @Override
  public boolean jdbcCompliant() {
    return true;
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    // There is no way to get it from wrapped driver
    return null;
  }

  private Driver findDriver(String realUrl) throws SQLException {

    Driver wrappedDriver = null;
    for (Driver driver : registeredDrivers()) {
      try {
        if (driver.acceptsURL(realUrl)) {
          wrappedDriver = driver;
          break;
        }
      } catch (SQLException e) {
        // intentionally ignore exception
      }
    }
    if (wrappedDriver == null) {
      throw new SQLException("Unable to find a driver that accepts " + realUrl);
    }
    return wrappedDriver;
  }

  /**
   * fixed
   * @param driverClassName
   * @return
   * @throws SQLException
     */
  private Driver createDriver(ClassLoader classLoader, String driverClassName) throws SQLException {
    Class<?> clazz = null;
    if (classLoader != null) {
      try {
        clazz = classLoader.loadClass(driverClassName);
      } catch (ClassNotFoundException e) {
        // skip
      }
    }

    if (clazz == null) {
      try {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null) {
          clazz = contextLoader.loadClass(driverClassName);
        }
      } catch (ClassNotFoundException e) {
        // skip
      }
    }

    if (clazz == null) {
      try {
        clazz = Class.forName(driverClassName);
      } catch (ClassNotFoundException e) {
        throw new SQLException(e.getMessage(), e);
      }
    }

    try {
      return (Driver) clazz.newInstance();
    } catch (IllegalAccessException e) {
      throw new SQLException(e.getMessage(), e);
    } catch (InstantiationException e) {
      throw new SQLException(e.getMessage(), e);
    }
  }

  private List<Driver> registeredDrivers() {
    List<Driver> result = new ArrayList<>();
    for (Enumeration<Driver> driverEnumeration = DriverManager.getDrivers();
        driverEnumeration.hasMoreElements(); ) {
      result.add(driverEnumeration.nextElement());
    }
    return result;
  }

  private String extractRealUrl(String url) {
    String extracted = url.startsWith("jdbc:tracing:") ? url.replace("tracing:", "") : url;
    return extracted.replaceAll(TRACE_WITH_ACTIVE_SPAN_ONLY + "=(true|false)[;]*", "")
        .replaceAll(IGNORE_FOR_TRACING_REGEX, "")
        .replaceAll("\\?$", "");
  }

  private String extractDbType(String realUrl) {
    return realUrl.split(":")[1];
  }

  private Set<String> extractIgnoredStatements(String url) {
    final String regex = IGNORE_FOR_TRACING_REGEX;

    final Pattern pattern = Pattern.compile(regex);
    final Matcher matcher = pattern.matcher(url);

    Set<String> results = new HashSet<>();

    while (matcher.find()) {
      String rawValue = matcher.group(1);
      String finalValue = rawValue.replace("\\\"", "\"");
      results.add(finalValue);
    }

    return results;
  }
}
