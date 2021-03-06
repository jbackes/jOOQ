/**
 * Copyright (c) 2009-2015, Data Geekery GmbH (http://www.datageekery.com)
 * All rights reserved.
 *
 * This work is dual-licensed
 * - under the Apache Software License 2.0 (the "ASL")
 * - under the jOOQ License and Maintenance Agreement (the "jOOQ License")
 * =============================================================================
 * You may choose which license applies to you:
 *
 * - If you're using this work with Open Source databases, you may choose
 *   either ASL or jOOQ License.
 * - If you're using this work with at least one commercial database, you must
 *   choose jOOQ License
 *
 * For more information, please visit http://www.jooq.org/licenses
 *
 * Apache Software License 2.0:
 * -----------------------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * jOOQ License and Maintenance Agreement:
 * -----------------------------------------------------------------------------
 * Data Geekery grants the Customer the non-exclusive, timely limited and
 * non-transferable license to install and use the Software under the terms of
 * the jOOQ License and Maintenance Agreement.
 *
 * This library is distributed with a LIMITED WARRANTY. See the jOOQ License
 * and Maintenance Agreement for more details: http://www.jooq.org/licensing
 */
package org.jooq.impl;

import static org.jooq.tools.reflect.Reflect.on;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import org.jooq.Binding;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingGetSQLInputContext;
import org.jooq.BindingGetStatementContext;
import org.jooq.BindingRegisterContext;
import org.jooq.BindingSQLContext;
import org.jooq.BindingSetSQLOutputContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Configuration;
import org.jooq.Converter;
import org.jooq.Converters;
import org.jooq.tools.jdbc.JDBCUtils;

/**
 * A binding that takes binary values but binds them as {@link Blob} to at the
 * JDBC level.
 * <p>
 * This is useful for workarounds for bugs in Oracle, like ORA-01461: can bind a
 * LONG value only for insert into a LONG column (see [#4091])
 *
 * @author Lukas Eder
 */
public class BlobBinding implements Binding<byte[], byte[]> {

    /**
     * Generated UID
     */
    private static final long serialVersionUID = 358789452467943117L;

    @Override
    public final Converter<byte[], byte[]> converter() {
        return Converters.identity(byte[].class);
    }

    @Override
    public final void sql(BindingSQLContext<byte[]> ctx) throws SQLException {
        ctx.render().sql("?");
    }

    @Override
    public final void register(BindingRegisterContext<byte[]> ctx) throws SQLException {
        ctx.statement().registerOutParameter(ctx.index(), Types.BLOB);
    }

    @Override
    public final void set(BindingSetStatementContext<byte[]> ctx) throws SQLException {
        Blob blob = newBlob(ctx.configuration(), ctx.value());
        DefaultExecuteContext.register(blob);
        ctx.statement().setBlob(ctx.index(), blob);
    }

    @Override
    public final void set(BindingSetSQLOutputContext<byte[]> ctx) throws SQLException {
        Blob blob = newBlob(ctx.configuration(), ctx.value());
        DefaultExecuteContext.register(blob);
        ctx.output().writeBlob(blob);
    }

    @Override
    public final void get(BindingGetResultSetContext<byte[]> ctx) throws SQLException {
        Blob blob = ctx.resultSet().getBlob(ctx.index());

        try {
            ctx.value(blob == null ? null : blob.getBytes(1, (int) blob.length()));
        }
        finally {
            JDBCUtils.safeFree(blob);
        }
    }

    @Override
    public final void get(BindingGetStatementContext<byte[]> ctx) throws SQLException {
        Blob blob = ctx.statement().getBlob(ctx.index());

        try {
            ctx.value(blob == null ? null : blob.getBytes(1, (int) blob.length()));
        }
        finally {
            JDBCUtils.safeFree(blob);
        }
    }

    @Override
    public final void get(BindingGetSQLInputContext<byte[]> ctx) throws SQLException {
        Blob blob = ctx.input().readBlob();

        try {
            ctx.value(blob == null ? null : blob.getBytes(1, (int) blob.length()));
        }
        finally {
            JDBCUtils.safeFree(blob);
        }
    }

    private final Blob newBlob(Configuration configuration, byte[] bytes) throws SQLException {
        Connection c = configuration.connectionProvider().acquire();

        try {
            Blob blob = null;

            switch (configuration.family()) {
                /* [pro] xx
                xx xxxxxxxx xxx x xxx xxxx xxxxxxx xxxxx xxxxx xxxxxxxxx
                xx xxxxxxxxxxxxxxxxxxxxxxxxxx
                xxxx xxxxxxx x
                    xxxx x xxxxxxxxxxxxxxxxxxxxx
                        xxxxxxxxxxxxxxxxxxxxxxxx xx xxxxxx xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        xxxxxxx
                    xxxxxx
                x
                    xx [/pro] */

                default: {
                    blob = c.createBlob();
                    break;
                }
            }

            blob.setBytes(1, bytes);
            return blob;
        }
        finally {
            configuration.connectionProvider().release(c);
        }
    }
}
