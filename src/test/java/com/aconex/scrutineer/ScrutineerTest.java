package com.aconex.scrutineer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.aconex.scrutineer.elasticsearch.ElasticSearchIdAndVersionStream;
import com.aconex.scrutineer.jdbc.JdbcIdAndVersionStream;

public class ScrutineerTest {

    @Mock
    private Scrutineer.ScrutineerCommandLineOptions options;

    @Mock
    private ElasticSearchIdAndVersionStream elasticSearchIdAndVersionStream;

    @Mock
    private JdbcIdAndVersionStream jdbcIdAndVersionStream;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExecute() {
        Scrutineer scrutineer = spy(new Scrutineer(options));
        doNothing().when(scrutineer).verify();

        Scrutineer.execute(scrutineer);
        verify(scrutineer).verify();
        verify(scrutineer).close();
    }

    @Test
    public void testVerify(){
        Scrutineer scrutineer = spy(new Scrutineer(options));
        doReturn(elasticSearchIdAndVersionStream).when(scrutineer).createElasticSearchIdAndVersionStream(eq(options));
        doReturn(jdbcIdAndVersionStream).when(scrutineer).createJdbcIdAndVersionStream(options);
        doNothing().when(scrutineer).verify(eq(elasticSearchIdAndVersionStream), eq(jdbcIdAndVersionStream), any(IdAndVersionStreamVerifier.class));
        scrutineer.verify();

        verify(scrutineer).verify(eq(elasticSearchIdAndVersionStream), eq(jdbcIdAndVersionStream), any(IdAndVersionStreamVerifier.class));

    }
    @Test
    public void testClose() throws Exception {

        Scrutineer scrutineer = spy(new Scrutineer(options));
        doNothing().when(scrutineer).closeJdbcConnection();
        doNothing().when(scrutineer).closeElasticSearchConnections();

        scrutineer.close();

        verify(scrutineer).closeJdbcConnection();
        verify(scrutineer).closeElasticSearchConnections();
    }
}
