[![Build Status][ci-img]][ci] [![Coverage Status][cov-img]][cov] [![Released Version][maven-img]][maven] [![Apache-2.0 license](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# OpenTracing JDBC Instrumentation

OpenTracing instrumentation for JDBC.

## Installation

pom.xml

```xml
<dependency>
    <groupId>io.opentracing.contrib</groupId>
    <artifactId>opentracing-jdbc</artifactId>
    <version>VERSION</version>
</dependency>
```

## Usage

### Non-interceptor

> Tracing for JDBC connections of URLs starting with `"jdbc:tracing:"`.

1. Activate tracing for JDBC connections by adding `tracing` to the JDBC url:

   _jdbc:**tracing**:h2:mem:test_

   To trace calls with active `Span`s only, set property `traceWithActiveSpanOnly=true`.

   _jdbc:**tracing**:h2:mem:test?**traceWithActiveSpanOnly=true**_

   To ignore specific queries (such as health checks), use the
   property `ignoreForTracing="SELECT 1"`. Double quotes can be escaped with `\`.

   `SELECT * FROM \"TEST\"`<br><sup>The property can be repeated for multiple statements.</sup>

2. Set driver class to `io.opentracing.contrib.jdbc.TracingDriver`.

   ```java
   Class.forName("io.opentracing.contrib.jdbc.TracingDriver");
   ```

   or

   ```java
   io.opentracing.contrib.jdbc.TracingDriver.load();
   ```

3. Instantiate tracer and register it with GlobalTracer.

   ```java
   // Instantiate tracer
   Tracer tracer = ...

   // Register tracer with GlobalTracer
   GlobalTracer.register(tracer);

   ```

### Interceptor

> Tracing for all JDBC connections without modifying the URL.

In "interceptor mode", the `TracingDriver` will intercept calls
to `DriverManager.getConnection(url,...)` for all URLs. The `TracingDriver` provides connections to
the `DriverManager` that are instrumented. Please note that the `TracingDriver` must be registered
before the underlying driver, It's recommended to turn on "interceptor mode" in the first place.

For standalone applications:

```java
public static void main(String[] args) {
   io.opentracing.contrib.jdbc.TracingDriver.setInterceptorMode(true);
   // some jdbc operation here
}

```

For web applications:

```java
public void contextInitialized(ServletContextEvent event) {
   io.opentracing.contrib.jdbc.TracingDriver.setInterceptorMode(true);
}
```

Or call `TracingDriver.ensureRegisteredAsTheFirstDriver()` along
with `TracingDriver.setInterceptorMode(true)` at any place, Please note driver like Oracle JDBC may
fail since it's destroyed forever after deregistration.

The `withActiveSpanOnly` and `ignoreStatements` properties for "interceptor mode" can be configured
with the `TracingDriver` via:

```java
// Set withActiveSpanOnly=true
TracingDriver.setInterceptorProperty(true);
```

and

```java
// Set ignoreStatements={"CREATE TABLE ignored (id INTEGER, TEST VARCHAR)"}
TracingDriver.setInterceptorProperty(Collections.singleton("CREATE TABLE ignored (id INTEGER, TEST VARCHAR)"));
```

### Hibernate

```xml
<hibernate-configuration>
    <session-factory>
        <property name="hibernate.connection.driver_class">io.opentracing.contrib.jdbc.TracingDriver</property>
        <property name="hibernate.connection.url">jdbc:tracing:mysql://localhost:3306/test</property>
        ...
    </session-factory>
    ...
</hibernate-configuration>
```

### JPA

```xml
<persistence-unit name="jpa">
    <properties>
        <property name="javax.persistence.jdbc.driver" value="io.opentracing.contrib.jdbc.TracingDriver"/>
        <property name="javax.persistence.jdbc.url" value="jdbc:tracing:mysql://localhost:3306/test"/>
        ...
    </properties>
</persistence-unit>
```

### Spring

For dbcp2:

```xml
<bean id="dataSource" destroy-method="close" class="org.apache.commons.dbcp2.BasicDataSource">
    <property name="driverClassName" value="io.opentracing.contrib.jdbc.TracingDriver"/>
    <property name="url" value="jdbc:tracing:mysql://localhost:3306/test"/>
    ...
</bean>

```

### Spring Boot 2

For Hikari (Postgresl):

```properties
### Spring JPA Datasource Connection
spring.datasource.username=postgres
spring.datasource.password=XXXXX
spring.datasource.hikari.driverClassName=io.opentracing.contrib.jdbc.TracingDriver
spring.datasource.hikari.jdbcUrl=jdbc:tracing:postgresql://localhost:5432/my_app_db

```

Configuration Bean:

```java

@Component
public class OpenTracingConfig {

  @Bean
  public io.opentracing.Tracer jaegerTracer() {
    io.opentracing.contrib.jdbc.TracingDriver.load();
    return new Configuration("my_app").getTracer();
  }
}

```

## Slow Query

Span is marked by tag `slow=true` if duration exceed `slowQueryThresholdMs`.
`slowQueryThresholdMs` defaults to `0` which means disabled, can be enabled in two ways:

1. Passing system property, E.g. `-Dio.opentracing.contrib.jdbc.slowQueryThresholdMs=100`
2. Modify value by code, E.g. `io.opentracing.contrib.jdbc.JdbcTracing.setSlowQueryThresholdMs(100)`

## Fast Query

Spans that complete faster than the optional `excludeFastQueryThresholdMs` flag will be not be
reported.
`excludeFastQueryThresholdMs` defaults to `0` which means disabled, can be enabled in two ways:

1. Passing system property, E.g. `-Dio.opentracing.contrib.jdbc.excludeFastQueryThresholdMs=100`
2. Modify value by code,
   E.g. `io.opentracing.contrib.jdbc.JdbcTracing.setExcludeFastQueryThresholdMs(100)`

## Troubleshooting

In case of _Unable to find a driver_ error the database driver should be registered before
configuring the datasource. E.g. `Class.forName("com.mysql.jdbc.Driver");`

## License

[Apache 2.0 License](./LICENSE).

[ci-img]: https://travis-ci.org/opentracing-contrib/java-jdbc.svg?branch=master

[ci]: https://travis-ci.org/opentracing-contrib/java-jdbc

[cov-img]: https://coveralls.io/repos/github/opentracing-contrib/java-jdbc/badge.svg?branch=master

[cov]: https://coveralls.io/github/opentracing-contrib/java-jdbc?branch=master

[maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-jdbc.svg

[maven]: http://search.maven.org/#search%7Cga%7C1%7Cio.opentracing.contrib%20opentracing-jdbc
