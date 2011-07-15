/*
 * Copyright 1999-2011 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.druid.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;

import com.alibaba.druid.logging.Log;
import com.alibaba.druid.logging.LogFactory;

/**
 * @author wenshao<szujobs@hotmail.com>
 */
public final class ConnectionHolder {
    private final static Log LOG = LogFactory.getLog(ConnectionHolder.class);
    
    private final DruidAbstractDataSource               dataSource;
    private final Connection                    conn;
    private final List<ConnectionEventListener> connectionEventListeners = new CopyOnWriteArrayList<ConnectionEventListener>();
    private final List<StatementEventListener>  statementEventListeners  = new CopyOnWriteArrayList<StatementEventListener>();
    private final long                          connecttimeMillis;
    private long                                lastActiveMillis;
    private long                                useCount                 = 0;

    private final boolean                       poolPreparedStatements;
    private final PreparedStatementPool         statementPool;

    private final List<PoolableStatement>       statementTrace           = new ArrayList<PoolableStatement>();

    public ConnectionHolder(DruidAbstractDataSource dataSource, Connection conn){
        this.dataSource = dataSource;
        this.conn = conn;
        this.poolPreparedStatements = dataSource.isPoolPreparedStatements();
        this.connecttimeMillis = System.currentTimeMillis();
        this.lastActiveMillis = connecttimeMillis;

        if (this.poolPreparedStatements) {
            statementPool = new PreparedStatementPool();
        } else {
            statementPool = null;
        }
    }

    public long getLastActiveMillis() {
        return lastActiveMillis;
    }

    public void setLastActiveMillis(long lastActiveMillis) {
        this.lastActiveMillis = lastActiveMillis;
    }

    public void addTrace(PoolableStatement stmt) {
        statementTrace.add(stmt);
    }

    public void removeTrace(PoolableStatement stmt) {
        statementTrace.remove(stmt);
    }

    public List<ConnectionEventListener> getConnectionEventListeners() {
        return connectionEventListeners;
    }

    public List<StatementEventListener> getStatementEventListeners() {
        return statementEventListeners;
    }

    public PreparedStatementPool getStatementPool() {
        return statementPool;
    }

    public DruidAbstractDataSource getDataSource() {
        return dataSource;
    }

    public boolean isPoolPreparedStatements() {
        return poolPreparedStatements;
    }

    public Connection getConnection() {
        return conn;
    }

    public long getTimeMillis() {
        return connecttimeMillis;
    }

    public long getUseCount() {
        return useCount;
    }

    public void incrementUseCount() {
        useCount++;
    }

    public void reset() {
        connectionEventListeners.clear();
        statementEventListeners.clear();

        
        for (Object item : statementTrace.toArray()) {
            PoolableStatement stmt = (PoolableStatement) item;
            try {
                if (!stmt.isClosed()) {
                    stmt.close();
                }
            } catch (SQLException ex) {
                LOG.error("close statement error", ex);
            }
        }
        statementTrace.clear();
    }
}
