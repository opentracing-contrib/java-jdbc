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
package io.opentracing.contrib.jdbc;

public class ConnectionInfo {

  private final String dbType;
  private final String dbUser;
  private final String dbInstance;
  private final String dbIp;
  private final Integer dbPort;

  private ConnectionInfo(String dbType, String dbUser, String dbInstance, String dbIp,
      Integer dbPort) {
    this.dbType = dbType;
    this.dbUser = dbUser;
    this.dbInstance = dbInstance;
    this.dbIp = dbIp;
    this.dbPort = dbPort;
  }

  public String getDbType() {
    return dbType;
  }

  public String getDbUser() {
    return dbUser;
  }

  public String getDbInstance() {
    return dbInstance;
  }

  public String getDbIp() {
    return dbIp;
  }

  public Integer getDbPort() {
    return dbPort;
  }

  public static class Builder {
    private String dbType;
    private String dbUser;
    private String dbInstance;
    private String dbIp;
    private Integer dbPort;

    public Builder() {
    }

    public Builder dbType(String dbType) {
      this.dbType = dbType;
      return this;
    }

    public Builder dbUser(String dbUser) {
      this.dbUser = dbUser;
      return this;
    }

    public Builder dbInstance(String dbInstance) {
      this.dbInstance = dbInstance;
      return this;
    }

    public Builder dbIp(String dbIp) {
      this.dbIp = dbIp;
      return this;
    }

    public Builder dbPort(Integer dbPort) {
      this.dbPort = dbPort;
      return this;
    }

    public ConnectionInfo build() {
      return new ConnectionInfo(this.dbType, this.dbUser, this.dbInstance, this.dbIp, this.dbPort);
    }

  }


}
