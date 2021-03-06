package com.aconex.scrutineer.elasticsearch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import com.aconex.scrutineer.ScrutineerCommandLineOptions;
import com.beust.jcommander.JCommander;
import org.elasticsearch.common.transport.TransportAddress;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class TransportAddressParserTest {

    @Test
    public void shouldParseValidHostPortPair() {
        TransportAddressParser transportAddressParser = new TransportAddressParser();
        TransportAddress transportAddress = transportAddressParser.convert("127.0.0.1:9300").get(0);
        assertThat(transportAddress.getAddress(), is("127.0.0.1"));
        assertThat(transportAddress.getPort(), is(9300));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfValueInvalid() {
        TransportAddressParser transportAddressParser = new TransportAddressParser();
        transportAddressParser.convert("thisisnotavalidhostname");
    }

    @Test
    public void shouldParseHostWithDefaultPort() {
        TransportAddressParser transportAddressParser = new TransportAddressParser();
        TransportAddress transportAddress = transportAddressParser.convert("127.0.0.1").get(0);
        assertThat(transportAddress.getAddress(), is("127.0.0.1"));
        assertThat(transportAddress.getPort(), is(9300));
    }

    @Test
    public void shouldConvertCSVListOfAddresses() throws UnknownHostException {
        TransportAddressParser.InetAddressResolver mockInetAddressResolver = mock(TransportAddressParser.InetAddressResolver.class);
        InetAddress mockItchy = mock(Inet4Address.class);
        InetAddress mockScratchy = mock(Inet4Address.class);
        when(mockItchy.getHostName()).thenReturn("itchy");
        when(mockScratchy.getHostName()).thenReturn("scratchy");

        doReturn(mockItchy).when(mockInetAddressResolver).resolveInetAddress("itchy");
        doReturn(mockScratchy).when(mockInetAddressResolver).resolveInetAddress("scratchy");

        TransportAddressParser transportAddressParser = new TransportAddressParser(mockInetAddressResolver);
        List<TransportAddress> transportAddresses = transportAddressParser.convert("itchy:9300,scratchy:9301");
        assertThat(transportAddresses.size(), is(2));

        TransportAddress first = transportAddresses.get(0);
        TransportAddress second = transportAddresses.get(1);
        assertThat(first.address().getHostName(), is("itchy"));
        assertThat(first.getPort(), is(9300));
        assertThat(second.address().getHostName(), is("scratchy"));
        assertThat(second.getPort(), is(9301));

    }

    @Test
    public void shouldCorrectlyParseWithJCommander() {
        String[] args = {"--esHosts", "localhost:9300,127.0.0.1:9301", "--sql", "select * from foo", "--jdbcUser", "sa", "--indexName", "myindex", "--jdbcURL", "blah", "--clusterName", "elasticsearch", "--jdbcDriverClass", "mydriver"};
        ScrutineerCommandLineOptions scrutineerCommandLineOptions = new ScrutineerCommandLineOptions();
        JCommander jCommander = new JCommander(scrutineerCommandLineOptions);
        jCommander.parse(args);
        assertThat(scrutineerCommandLineOptions.elasticSearchHosts.size(), is(2));

        // this is not a bug, the actual Java network code translates localhost =>127.0.0.1
        // this might prove to be a flakey test..
        assertThat(scrutineerCommandLineOptions.elasticSearchHosts.get(0).getAddress(), is("127.0.0.1"));
        assertThat(scrutineerCommandLineOptions.elasticSearchHosts.get(0).getPort(), is(9300));

        assertThat(scrutineerCommandLineOptions.elasticSearchHosts.get(1).getAddress(), is("127.0.0.1"));
        assertThat(scrutineerCommandLineOptions.elasticSearchHosts.get(1).getPort(), is(9301));
    }



}