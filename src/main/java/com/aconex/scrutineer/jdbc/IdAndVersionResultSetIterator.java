package com.aconex.scrutineer.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;

import com.aconex.scrutineer.IdAndVersion;
import com.aconex.scrutineer.IdAndVersionFactory;

import org.apache.commons.lang.NotImplementedException;

public class IdAndVersionResultSetIterator implements Iterator<IdAndVersion> {

    private final ResultSet resultSet;
	private final IdAndVersionFactory factory;

    private final int idColumnType;
    private final int versionColumnType;

    private IdAndVersion current;

    public IdAndVersionResultSetIterator(ResultSet resultSet, IdAndVersionFactory factory) {
        this.resultSet = resultSet;
        this.factory = factory;
        try {
            this.idColumnType = resultSet.getMetaData().getColumnType(1);
            this.versionColumnType = resultSet.getMetaData().getColumnType(2);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        nextRow();
    }

    @Override
    public boolean hasNext() {
        return current != null;
    }

    @Override
    public IdAndVersion next() {
        try {
            return current;
        } finally {
            nextRow();
        }
    }

    @Override
    public void remove() {
        throw new NotImplementedException();
    }

    // TODO talk to Leon about this Cyclomatic Complexity for checkstyle
    //CHECKSTYLE:OFF
    @SuppressWarnings("PMD.NcssMethodCount")
    private Object getIdValue() throws SQLException {
        switch (this.idColumnType) {
        case Types.BIGINT:
        case Types.INTEGER:
            return resultSet.getLong(1);
        default:
            return resultSet.getString(1);
        }
	}

    @SuppressWarnings("PMD.NcssMethodCount")
    private long getVersionValueAsLong() throws SQLException {
        switch (this.versionColumnType) {
            case Types.TIMESTAMP:
                return resultSet.getTimestamp(2).getTime();

            case Types.BIGINT:
            case Types.INTEGER:
                return resultSet.getLong(2);
            default:
                throw new UnsupportedOperationException(String.format("Do not know how to handle version column type (java.sql.Type value=%d", versionColumnType));
        }
    }
    //CHECKSTYLE:ON

    @SuppressWarnings("PMD.NcssMethodCount")
    private void nextRow() {
        try {
            if (resultSet.next()) {
                current = factory.create(getIdValue(), getVersionValueAsLong());
            } else {
                current = null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

	ResultSet getResultSet() {
        return resultSet;
    }
}
