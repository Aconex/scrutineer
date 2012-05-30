package com.aconex.scrutineer;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Comparator;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

import com.aconex.scrutineer.elasticsearch.ElasticSearchDownloader;
import com.aconex.scrutineer.elasticsearch.ElasticSearchIdAndVersionStream;
import com.aconex.scrutineer.elasticsearch.ElasticSearchSorter;
import com.aconex.scrutineer.elasticsearch.IdAndVersionDataReaderFactory;
import com.aconex.scrutineer.elasticsearch.IdAndVersionDataWriterFactory;
import com.aconex.scrutineer.elasticsearch.IteratorFactory;
import com.aconex.scrutineer.javautil.NumericIdAndVersionComparator;
import com.aconex.scrutineer.javautil.StringIdAndVersionComparator;
import com.aconex.scrutineer.jdbc.JdbcIdAndVersionStream;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.sort.DataReaderFactory;
import com.fasterxml.sort.DataWriterFactory;
import com.fasterxml.sort.SortConfig;
import com.fasterxml.sort.Sorter;

public class Scrutineer {

    public static void main(String[] args) {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
        LogManager.getLoggerRepository().setThreshold(Level.INFO);
        Scrutineer scrutineer = new Scrutineer(parseOptions(args));
        execute(scrutineer);
    }

    static void execute(Scrutineer scrutineer) {
        try {
            scrutineer.verify();
        } finally {
            scrutineer.close();
        }
    }

    private static ScrutineerCommandLineOptions parseOptions(String[] args) {
        ScrutineerCommandLineOptions options = new ScrutineerCommandLineOptions();
        new JCommander(options, args);
        return options;
    }

    public void verify() {
    	if (options.numeric) {
    		verify(new NumericIdAndVersionComparator());
    	} else {
    		verify(new StringIdAndVersionComparator());
    	}
    }

    public void verify(Comparator<IdAndVersion> comparator) {
        ElasticSearchIdAndVersionStream elasticSearchIdAndVersionStream = createElasticSearchIdAndVersionStream(options, comparator);
        JdbcIdAndVersionStream jdbcIdAndVersionStream = createJdbcIdAndVersionStream(options);

        verify(elasticSearchIdAndVersionStream, jdbcIdAndVersionStream, new IdAndVersionStreamVerifier(comparator));
    }

    void close() {
        closeJdbcConnection();
        closeElasticSearchConnections();
    }

    void closeElasticSearchConnections() {
        if (client != null) {
            client.close();
        }
        if (node != null) {
            node.close();
        }
    }

    void closeJdbcConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    void verify(ElasticSearchIdAndVersionStream elasticSearchIdAndVersionStream, JdbcIdAndVersionStream jdbcIdAndVersionStream, IdAndVersionStreamVerifier idAndVersionStreamVerifier) {
        idAndVersionStreamVerifier.verify(jdbcIdAndVersionStream, elasticSearchIdAndVersionStream, new PrintStreamOutputVersionStreamVerifierListener(System.err));
    }


    public Scrutineer(ScrutineerCommandLineOptions options) {
        this.options = options;
    }

    ElasticSearchIdAndVersionStream createElasticSearchIdAndVersionStream(ScrutineerCommandLineOptions options, Comparator<IdAndVersion> comparator) {
        this.node = nodeBuilder().client(true).clusterName(options.clusterName).node();
        this.client = node.client();
        return new ElasticSearchIdAndVersionStream(new ElasticSearchDownloader(client, options.indexName, options.query), new ElasticSearchSorter(createSorter(comparator)), new IteratorFactory(), SystemUtils.getJavaIoTmpDir().getAbsolutePath());
    }

    private Sorter<IdAndVersion> createSorter(Comparator<IdAndVersion> comparator) {
        SortConfig sortConfig = new SortConfig().withMaxMemoryUsage(DEFAULT_SORT_MEM);
        DataReaderFactory<IdAndVersion> dataReaderFactory = new IdAndVersionDataReaderFactory();
        DataWriterFactory<IdAndVersion> dataWriterFactory = new IdAndVersionDataWriterFactory();
        return new Sorter<IdAndVersion>(sortConfig, dataReaderFactory, dataWriterFactory, comparator);
    }

    JdbcIdAndVersionStream createJdbcIdAndVersionStream(ScrutineerCommandLineOptions options) {
        this.connection = initializeJdbcDriverAndConnection(options);
        return new JdbcIdAndVersionStream(connection, options.sql);
    }

    private Connection initializeJdbcDriverAndConnection(ScrutineerCommandLineOptions options) {
        try {
            Class.forName(options.jdbcDriverClass).newInstance();
            return DriverManager.getConnection(options.jdbcURL, options.jdbcUser, options.jdbcPassword);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static final int DEFAULT_SORT_MEM = 256 * 1024 * 1024;
    private final ScrutineerCommandLineOptions options;
    private Node node;
    private Client client;
    private Connection connection;

    // CHECKSTYLE:OFF This is the standard JCommander pattern
    @Parameters(separators = "=")
    public static class ScrutineerCommandLineOptions {
        @Parameter(names = "--clusterName", description = "ElasticSearch cluster name identifier", required = true)
        public String clusterName;

        @Parameter(names = "--indexName", description = "ElasticSearch index name to Verify", required = true)
        public String indexName;

        @Parameter(names = "--query", description = "ElasticSearch query to create Secondary stream.  Not required to be ordered", required = false)
        public String query = "*";

        @Parameter(names = "--jdbcDriverClass", description = "FQN of the JDBC Driver class", required = true)
        public String jdbcDriverClass;

        @Parameter(names = "--jdbcURL", description = "JDBC URL of the Connection of the Primary source", required = true)
        public String jdbcURL;

        @Parameter(names = "--jdbcUser", description = "JDBC Username", required = true)
        public String jdbcUser;

        @Parameter(names = "--jdbcPassword", description = "JDBC Password", required = false)
        public String jdbcPassword;

        @Parameter(names = "--sql", description = "SQL used to create Primary stream, which should return results in _lexicographical_ order", required = true)
        public String sql;

        @Parameter(names = "--numeric", description = "JDBC query is sorted numerically")
        public boolean numeric = false;
    }
    // CHECKSTYLE:ON

}
