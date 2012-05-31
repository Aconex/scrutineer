package com.aconex.scrutineer.jdbc;

import static com.aconex.scrutineer.HasIdAndVersionMatcher.hasIdAndVersion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.aconex.scrutineer.IdAndVersionFactory;
import com.aconex.scrutineer.StringIdAndVersion;

public class IdAndVersionResultSetIteratorTest {

    private static final String ID = "ID";
    private static final long VERSION = 123L;

	private IdAndVersionFactory idAndVersionFactory = StringIdAndVersion.FACTORY;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSetMetaData metaData;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void shouldReturnNextIfResultSetHasMoreResults() throws SQLException {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnType(1)).thenReturn(Types.VARCHAR);
        when(metaData.getColumnType(2)).thenReturn(Types.TIMESTAMP);
        when(resultSet.getString(1)).thenReturn(ID);
        when(resultSet.getTimestamp(2)).thenReturn(new Timestamp(VERSION));

        IdAndVersionResultSetIterator idAndVersionResultSetIterator = new IdAndVersionResultSetIterator(resultSet, idAndVersionFactory);
        assertThat(idAndVersionResultSetIterator.hasNext(), is(true));
    }

    @Test
    public void shouldReturnFalseIfResultSetHasNoMoreResults() throws SQLException {
        when(resultSet.next()).thenReturn(false);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnType(1)).thenReturn(Types.VARCHAR);
        when(metaData.getColumnType(2)).thenReturn(Types.TIMESTAMP);
        when(resultSet.getString(1)).thenReturn(ID);
        when(resultSet.getTimestamp(2)).thenReturn(new Timestamp(VERSION));

        IdAndVersionResultSetIterator idAndVersionResultSetIterator = new IdAndVersionResultSetIterator(resultSet, idAndVersionFactory);
        assertThat(idAndVersionResultSetIterator.hasNext(), is(false));
    }

    @Test
    public void shouldGetTheNextIdAndVersion() throws SQLException {
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnType(1)).thenReturn(Types.VARCHAR);
        when(metaData.getColumnType(2)).thenReturn(Types.TIMESTAMP);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString(1)).thenReturn(ID);
        when(resultSet.getTimestamp(2)).thenReturn(new Timestamp(VERSION));

        IdAndVersionResultSetIterator idAndVersionResultSetIterator = new IdAndVersionResultSetIterator(resultSet, idAndVersionFactory);
        assertThat(idAndVersionResultSetIterator.next(), hasIdAndVersion(ID,VERSION));
        assertThat(idAndVersionResultSetIterator.hasNext(), is(false));
    }

    @Test
    public void shouldSupportBothDatesAndLongVersions() throws SQLException {
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnType(1)).thenReturn(Types.VARCHAR);
        when(metaData.getColumnType(2)).thenReturn(Types.BIGINT);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString(1)).thenReturn(ID);
        when(resultSet.getLong(2)).thenReturn(VERSION);

        IdAndVersionResultSetIterator idAndVersionResultSetIterator = new IdAndVersionResultSetIterator(resultSet, idAndVersionFactory);
        assertThat(idAndVersionResultSetIterator.next(), hasIdAndVersion(ID,VERSION));
        assertThat(idAndVersionResultSetIterator.hasNext(), is(false));
    }
}
