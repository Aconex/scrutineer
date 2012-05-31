package com.aconex.scrutineer.elasticsearch;

import static com.aconex.scrutineer.elasticsearch.ElasticSearchDownloader.BATCH_SIZE;
import static com.aconex.scrutineer.elasticsearch.ElasticSearchDownloader.SCROLL_TIME_IN_MINUTES;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.io.ObjectOutputStream;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.aconex.scrutineer.IdAndVersionFactory;
import com.aconex.scrutineer.StringIdAndVersion;

@SuppressWarnings("unchecked")
public class ElasticSearchDownloaderTest {

    private static final String INDEX_NAME = "indexName";
    private static final String ID = "123";
    private static final long VERSION = 123L;
    private static final String QUERY = "*";

    private final IdAndVersionFactory idAndVersionFactory = StringIdAndVersion.FACTORY;
    @Mock
    private Client client;
    @Mock
    private SearchRequestBuilder searchRequestBuilder;
    @Mock
    private SearchScrollRequestBuilder searchScrollRequestBuilder;
    @Mock
    private ListenableActionFuture listenableActionFuture;
    @Mock
    private SearchHits hits;
    @Mock
    private SearchHit hit;
    @Mock
    private SearchResponse searchResponse;
    @Mock
    private ObjectOutputStream objectOutputStream;
    @Mock
    private QueryStringQueryBuilder queryBuilder;

    @Before public void setup() {
        initMocks(this);
    }

    @Test
    public void shouldEndAfterOnlyOneBatch() throws IOException {
        ElasticSearchDownloader elasticSearchDownloader = spy(new ElasticSearchDownloader(client, INDEX_NAME, QUERY, idAndVersionFactory));
        doReturn(false).when(elasticSearchDownloader).writeSearchResponseToOutputStream(any(ObjectOutputStream.class),any(SearchResponse.class));
        when(client.prepareSearchScroll(any(String.class))).thenReturn(searchScrollRequestBuilder);
        when(searchScrollRequestBuilder.execute()).thenReturn(listenableActionFuture);
        when(searchScrollRequestBuilder.setScroll(any(TimeValue.class))).thenReturn(searchScrollRequestBuilder);
        when(listenableActionFuture.actionGet()).thenReturn(searchResponse);
        elasticSearchDownloader.consumeBatches(eq(objectOutputStream), anyString());
        verify(client).prepareSearchScroll(any(String.class));
    }

    @Test
    public void shouldRequestAndProcessNextBatch() throws IOException {
        ElasticSearchDownloader elasticSearchDownloader = spy(new ElasticSearchDownloader(client, INDEX_NAME, QUERY, idAndVersionFactory));
        doReturn(true).doReturn(false).when(elasticSearchDownloader).writeSearchResponseToOutputStream(any(ObjectOutputStream.class),any(SearchResponse.class));
        when(client.prepareSearchScroll(any(String.class))).thenReturn(searchScrollRequestBuilder);
        when(searchScrollRequestBuilder.execute()).thenReturn(listenableActionFuture);
        when(searchScrollRequestBuilder.setScroll(any(TimeValue.class))).thenReturn(searchScrollRequestBuilder);
        when(listenableActionFuture.actionGet()).thenReturn(searchResponse);
        elasticSearchDownloader.consumeBatches(eq(objectOutputStream), anyString());
        verify(client,times(2)).prepareSearchScroll(any(String.class));
        verify(searchResponse, times(2)).getScrollId();
    }


    @Test
    public void shouldShouldReturnFalseWhenBatchIsEmpty() throws IOException {
        ElasticSearchDownloader elasticSearchDownloader = new ElasticSearchDownloader(client, INDEX_NAME, QUERY, idAndVersionFactory);
        when(searchResponse.getHits()).thenReturn(hits);
        when(hits.hits()).thenReturn(new SearchHit[0]);
        assertThat(elasticSearchDownloader.writeSearchResponseToOutputStream(objectOutputStream, searchResponse), is(false));
    }
    @Test
    public void shouldWriteHitsToOutputStream() throws IOException {
        ElasticSearchDownloader elasticSearchDownloader = new ElasticSearchDownloader(client, INDEX_NAME, QUERY, idAndVersionFactory);
        when(searchResponse.getHits()).thenReturn(hits);
        when(hits.hits()).thenReturn(new SearchHit[]{hit});
        when(hit.getId()).thenReturn(ID);
        when(hit.getVersion()).thenReturn(VERSION);
        assertThat(elasticSearchDownloader.writeSearchResponseToOutputStream(objectOutputStream, searchResponse), is(true));
        verify(objectOutputStream).writeUTF(ID);
        verify(objectOutputStream).writeLong(VERSION);
        verifyNoMoreInteractions(objectOutputStream);
    }

    @Test
    public void shouldDoElasticSearchRequest() {
        when(client.prepareSearch(INDEX_NAME)).thenReturn(searchRequestBuilder);
        when(searchRequestBuilder.execute()).thenReturn(listenableActionFuture);
        when(listenableActionFuture.actionGet()).thenReturn(searchResponse);
        ElasticSearchDownloader elasticSearchDownloader = spy(new ElasticSearchDownloader(client, INDEX_NAME, QUERY, idAndVersionFactory));
        doReturn(queryBuilder).when(elasticSearchDownloader).createQuery();
        assertThat(elasticSearchDownloader.startScroll(), is(searchResponse));
        verify(searchRequestBuilder).setSearchType(SearchType.SCAN);
        verify(searchRequestBuilder).setNoFields();
        verify(searchRequestBuilder).setVersion(true);
        verify(searchRequestBuilder).setSize(BATCH_SIZE);
        verify(searchRequestBuilder).setScroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES));
        verify(searchRequestBuilder).setQuery(queryBuilder);
    }

    @Test
    public void shouldCreateQueryBuilderWithQuery() {
        ElasticSearchDownloader elasticSearchDownloader = spy(new ElasticSearchDownloader(client, INDEX_NAME, QUERY, idAndVersionFactory));
        assertThat(elasticSearchDownloader.createQuery().toString(), containsString(QUERY));
    }

    @Test
    public void shouldCreateQueryBuilderWithDefaultAllField() {
        ElasticSearchDownloader elasticSearchDownloader = spy(new ElasticSearchDownloader(client, INDEX_NAME, QUERY, idAndVersionFactory));
        assertThat(elasticSearchDownloader.createQuery().toString(), containsString("_all"));
    }

    @Test
    public void shouldCreateQueryBuilderWithDefaultAndOperator() {
        ElasticSearchDownloader elasticSearchDownloader = spy(new ElasticSearchDownloader(client, INDEX_NAME, QUERY, idAndVersionFactory));
        assertThat(elasticSearchDownloader.createQuery().toString(), containsString("and"));
    }
}
