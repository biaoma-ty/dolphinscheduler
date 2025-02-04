/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.plugin.datasource.api.client;

import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.datasource.DataSourceClient;
import org.apache.dolphinscheduler.spi.enums.DbType;

import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Stopwatch;

@Slf4j
public class CommonDataSourceClient implements DataSourceClient {

    public static final String COMMON_USER = "root";
    public static final String COMMON_VALIDATION_QUERY = "select 1";

    protected final BaseConnectionParam baseConnectionParam;
    protected Connection connection;

    public CommonDataSourceClient(BaseConnectionParam baseConnectionParam, DbType dbType) {
        this.baseConnectionParam = baseConnectionParam;
        preInit();
        checkEnv(baseConnectionParam);
        initClient(baseConnectionParam, dbType);
        checkClient();
    }

    protected void preInit() {
        log.info("preInit in CommonDataSourceClient");
    }

    protected void checkEnv(BaseConnectionParam baseConnectionParam) {
        checkValidationQuery(baseConnectionParam);
        checkUser(baseConnectionParam);
    }

    protected void initClient(BaseConnectionParam baseConnectionParam, DbType dbType) {
        this.connection = buildConn(baseConnectionParam);
    }

    protected void checkUser(BaseConnectionParam baseConnectionParam) {
        if (StringUtils.isBlank(baseConnectionParam.getUser())) {
            setDefaultUsername(baseConnectionParam);
        }
    }

    private Connection buildConn(BaseConnectionParam baseConnectionParam) {
        Connection conn = null;
        try {
            Class.forName(baseConnectionParam.getDriverClassName());
            conn = DriverManager.getConnection(baseConnectionParam.getJdbcUrl(), baseConnectionParam.getUser(),
                    baseConnectionParam.getPassword());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver load fail", e);
        } catch (SQLException e) {
            throw new RuntimeException("JDBC connect failed", e);
        }
        return conn;
    }

    protected void setDefaultUsername(BaseConnectionParam baseConnectionParam) {
        baseConnectionParam.setUser(COMMON_USER);
    }

    protected void checkValidationQuery(BaseConnectionParam baseConnectionParam) {
        if (StringUtils.isBlank(baseConnectionParam.getValidationQuery())) {
            setDefaultValidationQuery(baseConnectionParam);
        }
    }

    protected void setDefaultValidationQuery(BaseConnectionParam baseConnectionParam) {
        baseConnectionParam.setValidationQuery(COMMON_VALIDATION_QUERY);
    }

    @Override
    public void checkClient() {
        // Checking data source client
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            this.connection.prepareStatement(this.baseConnectionParam.getValidationQuery()).executeQuery();
        } catch (Exception e) {
            throw new RuntimeException("JDBC connect failed", e);
        } finally {
            log.info("Time to execute check jdbc client with sql {} for {} ms ",
                    this.baseConnectionParam.getValidationQuery(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public Connection getConnection() {
        try {
            return connection.isClosed() ? buildConn(baseConnectionParam) : connection;
        } catch (SQLException e) {
            throw new RuntimeException("get conn is fail", e);
        }
    }

    @Override
    public void close() {
        log.info("do close connection {}.", baseConnectionParam.getDatabase());
        try {
            connection.close();
        } catch (SQLException e) {
            log.info("colse connection fail");
            throw new RuntimeException(e);
        }
    }

}
