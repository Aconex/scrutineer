package com.aconex.scrutineer.elasticsearch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.aconex.scrutineer.IdAndVersion;
import com.aconex.scrutineer.IdAndVersionFactory;
import com.aconex.scrutineer.StringIdAndVersion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class IdAndVersionDataReaderTest {

    private static final String ID = "77";
    private static final long VERSION = 12;
    @Mock
    private ObjectInputStream objectInputStream;
    private IdAndVersionDataReader idAndVersionDataReader;
	private final IdAndVersionFactory idAndVersionFactory = StringIdAndVersion.FACTORY;


    @Before
    public void setup() {
        initMocks(this);
        idAndVersionDataReader = new IdAndVersionDataReader(idAndVersionFactory , objectInputStream);
    }

    @Test
    public void shouldGiveAndEstimateOfSize() {
        assertThat(idAndVersionDataReader.estimateSizeInBytes(new StringIdAndVersion(ID, VERSION)), is(ID.length() * 2 + 84));
    }

    @Test
    public void shouldReadNextIdAndVersionObjectFromStream() throws IOException {
        when(objectInputStream.readUTF()).thenReturn(ID);
        when(objectInputStream.readLong()).thenReturn(VERSION);
        IdAndVersion idAndVersion = idAndVersionDataReader.readNext();
        assertThat(idAndVersion.getId(), is(ID));
        assertThat(idAndVersion.getVersion(), is(VERSION));
    }

    @Test
    public void shouldReturnNullOnEndOfStream() throws IOException {
        when(objectInputStream.readLong()).thenThrow(new EOFException());
        assertThat(idAndVersionDataReader.readNext(), is(nullValue()));
    }

    @Test
    public void shouldCloseStream() throws IOException {
        idAndVersionDataReader.close();
        verify(objectInputStream).close();
    }
}
