package com.anair.demo.component.movieratingsplitter.route;

import com.anair.demo.component.movieratingsplitter.config.SplitterConfig;
import com.anair.demo.component.movieratingsplitter.model.FilmRating;
import com.anair.demo.component.movieratingsplitter.model.Movie;
import com.anair.demo.component.movieratingsplitter.service.MessageProcessor;
import com.anair.demo.component.movieratingsplitter.service.RatingsTransformer;
import com.anair.demo.component.movieratingsplitter.util.JSONUtil;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.ExchangeTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Iterator;
import java.util.Properties;

import static com.anair.demo.component.movieratingsplitter.util.SplitterConstants.*;
import static com.anair.demo.component.movieratingsplitter.util.SplitterHelper.buildURIParamsUsing;
import static com.anair.demo.component.movieratingsplitter.util.TestFileUtils.getTestResourceAsString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SplitterRouteTest extends ExchangeTestSupport {

    private static final String MOVIE_WITH_MULTIPLE_RATINGS = getTestResourceAsString("test-json-files/movie-with-multiple-ratings.json");
    private static final String UNRATED_MOVIE = getTestResourceAsString("test-json-files/movie-with-no-rating.json");

    private static final String MOCK_PROCESS_MESSAGE = "mock:processMessage";
    private static final String MOCK_TRANSFORM_TO_FILM_RATINGS = "mock:transformToFilmRatings";
    private static final String MOCK_KAFKA_PRODUCER = "mock:kafkaProducer";

    private MessageProcessor messageProcessor = new MessageProcessor();
    private RatingsTransformer ratingsTransformer = new RatingsTransformer();

    @Mock
    private SplitterConfig splitterConfig;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        super.setUp();
        context.getShutdownStrategy().setTimeout(1L);
        context.getShutdownStrategy().setShutdownNowOnTimeout(true);
    }

    @After
    public void tearDown() throws Exception {
        context.stop();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        Properties producerConfigProperties = new Properties();
        producerConfigProperties.put("brokers", "localhost:9092");

        Properties consumerConfigProperties = new Properties();
        consumerConfigProperties.put("brokers", "localhost:9092");
        consumerConfigProperties.put("groupId", "testConsumerGroup");

        when(splitterConfig.getConsumerTopic()).thenReturn("testConsumerTopic");
        when(splitterConfig.getProducerTopic()).thenReturn("testProducerTopic");
        when(splitterConfig.getProducerConfig()).thenReturn(producerConfigProperties);
        when(splitterConfig.getConsumerConfig()).thenReturn(consumerConfigProperties);

        return new SplitterRoute(splitterConfig, messageProcessor, ratingsTransformer);
    }

    @Test
    public void should_process_Movie_with_multiple_ratings_input_message() throws Exception {

        context.getRouteDefinition(PROCESS_MESSAGE_ID)
                .adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() {
                        interceptSendToEndpoint(TRANSFORM_TO_FILM_RATINGS)
                                .skipSendToOriginalEndpoint()
                                .to(MOCK_PROCESS_MESSAGE);
                    }

                });

        context.start();

        sendBody(PROCESS_MESSAGE, MOVIE_WITH_MULTIPLE_RATINGS);

        MockEndpoint mockProcessMessage = getMockEndpoint(MOCK_PROCESS_MESSAGE);
        assertMockEndpointsSatisfied();
        MockEndpoint.expectsMessageCount(1, mockProcessMessage);
        mockProcessMessage.expectedPropertyReceived(MOVIE_TITLE, "Superman");
        mockProcessMessage.expectedPropertyReceived(NUM_RATINGS, 3);
    }

    @Test
    public void should_transform_unrated_Movie_to_FilmRatings() throws Exception {

        context.getRouteDefinition(TRANSFORM_TO_FILM_RATINGS_ID)
                .adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() {
                        interceptSendToEndpoint(SPLIT_AND_WRITE_TO_KAFKA_TOPIC)
                                .skipSendToOriginalEndpoint()
                                .to(MOCK_TRANSFORM_TO_FILM_RATINGS);
                    }

                });
        context.start();

        exchange.getIn().setBody(UNRATED_MOVIE, String.class);
        MessageProcessor messageProcessor = new MessageProcessor();
        messageProcessor.process(exchange);

        sendBody(TRANSFORM_TO_FILM_RATINGS,  exchange.getIn().getBody(Movie.class));

        MockEndpoint mockTransformToFilmRatings = getMockEndpoint(MOCK_TRANSFORM_TO_FILM_RATINGS);
        assertMockEndpointsSatisfied();
        MockEndpoint.expectsMessageCount(1, mockTransformToFilmRatings);
        mockTransformToFilmRatings.expectedPropertyReceived(MOVIE_TITLE, "Deep Blue Sea");
        mockTransformToFilmRatings.expectedPropertyReceived(NUM_RATINGS, 0);

    }

    @Test
    public void should_split_one_Movie_with_multiple_ratings_to_multiple_FilmRatings() throws Exception {

        String toKafkaTopic = "kafka:" + splitterConfig.getProducerTopic() + "?" + buildURIParamsUsing(splitterConfig.getProducerConfig());

        context.getRouteDefinition(SPLIT_AND_WRITE_TO_KAFKA_TOPIC_ID)
                .adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() {
                        interceptSendToEndpoint(toKafkaTopic)
                                .skipSendToOriginalEndpoint()
                                .to(MOCK_KAFKA_PRODUCER);
                    }

                });
        context.start();

        Movie movieWithMultipleRatings = JSONUtil.fromJsonString(MOVIE_WITH_MULTIPLE_RATINGS, Movie.class);
        exchange.getIn().setBody(movieWithMultipleRatings, Movie.class);
        exchange.setProperty(MOVIE_TITLE, movieWithMultipleRatings.getTitle());
        exchange.setProperty(NUM_RATINGS, movieWithMultipleRatings.getRatings().size());
        RatingsTransformer ratingsTransformer = new RatingsTransformer();
        ratingsTransformer.process(exchange);
        Iterator<String> filmRatingsIterator = exchange.getIn().getBody(Iterator.class);

        sendBody(SPLIT_AND_WRITE_TO_KAFKA_TOPIC, filmRatingsIterator);

        MockEndpoint mockTransformToFilmRatings = getMockEndpoint(MOCK_KAFKA_PRODUCER);
        assertMockEndpointsSatisfied();
        MockEndpoint.expectsMessageCount(3, mockTransformToFilmRatings);
        assertEquals(3, mockTransformToFilmRatings.getExchanges().size());
        for(Exchange responseExchange: mockTransformToFilmRatings.getExchanges()) {
            String filmRatingAsString = responseExchange.getIn().getBody(String.class);
            assertNotNull(filmRatingAsString);
            FilmRating filmRating = JSONUtil.fromJsonString(filmRatingAsString, FilmRating.class);
            assertNotNull(filmRating);
            assertEquals("Superman", filmRating.getFilm().getTitle());
        }

    }

    @Test
    public void should_not_process_non_Movie_message() throws Exception {

        context.getRouteDefinition(PROCESS_MESSAGE_ID)
                .adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() {
                        interceptSendToEndpoint(TRANSFORM_TO_FILM_RATINGS)
                                .skipSendToOriginalEndpoint()
                                .to(MOCK_TRANSFORM_TO_FILM_RATINGS);
                    }

                });

        context.start();

        String nonMovieMessage = "{ \"hello\": \"world\"}";
        sendBody(PROCESS_MESSAGE, nonMovieMessage);

        MockEndpoint mockTransformToFilmRatings = getMockEndpoint(MOCK_TRANSFORM_TO_FILM_RATINGS);
        assertMockEndpointsSatisfied();
        assertEquals(0, mockTransformToFilmRatings.getExchanges().size());
        mockTransformToFilmRatings.expectedMessageCount(0);

    }

    @Test
    public void should_not_process_invalid_message() throws Exception {

        context.getRouteDefinition(PROCESS_MESSAGE_ID)
                .adviceWith(context, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() {
                        interceptSendToEndpoint(TRANSFORM_TO_FILM_RATINGS)
                                .skipSendToOriginalEndpoint()
                                .to(MOCK_TRANSFORM_TO_FILM_RATINGS);
                    }

                });

        context.start();

        String invalidMessage = "Hello World!";
        sendBody(PROCESS_MESSAGE, invalidMessage);

        MockEndpoint mockTransformToFilmRatings = getMockEndpoint(MOCK_TRANSFORM_TO_FILM_RATINGS);
        assertMockEndpointsSatisfied();
        assertEquals(0, mockTransformToFilmRatings.getExchanges().size());
        mockTransformToFilmRatings.expectedMessageCount(0);

    }
}