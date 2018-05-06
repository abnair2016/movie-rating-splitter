package com.anair.demo.component.movieratingsplitter.service;

import com.anair.demo.component.movieratingsplitter.exception.SplitterException;
import com.anair.demo.component.movieratingsplitter.model.Movie;
import org.apache.camel.Exchange;
import org.apache.camel.test.junit4.ExchangeTestSupport;
import org.junit.Before;
import org.junit.Test;

import static com.anair.demo.component.movieratingsplitter.util.SplitterConstants.MOVIE_TITLE;
import static com.anair.demo.component.movieratingsplitter.util.SplitterConstants.NUM_RATINGS;
import static com.anair.demo.component.movieratingsplitter.util.TestFileUtils.getTestResourceAsString;
import static org.mockito.MockitoAnnotations.initMocks;

public class MessageProcessorTest extends ExchangeTestSupport {

    private static final String MOVIE_WITH_MULTIPLE_RATINGS = getTestResourceAsString("test-json-files/movie-with-multiple-ratings.json");
    private static final String MOVIE_WITH_SINGLE_RATING = getTestResourceAsString("test-json-files/movie-with-single-rating.json");
    private static final String UNRATED_MOVIE = getTestResourceAsString("test-json-files/movie-with-no-rating.json");

    private MessageProcessor messageProcessor;
    private Exchange exchange;

    @Before
    public void setUp() {
        initMocks(this);
        messageProcessor = new MessageProcessor();
    }

    @Test
    public void should_process_valid_movie_with_single_rating() throws Exception {

        exchange = createExchangeWithBody(createCamelContext(), MOVIE_WITH_SINGLE_RATING);

        messageProcessor.process(exchange);

        assertNotNull(exchange.getIn().getBody(Movie.class));
        assertEquals("Crouching Tiger, Hidden Dragon", exchange.getProperty(MOVIE_TITLE));
        assertEquals(1, exchange.getProperty(NUM_RATINGS));
        assertEquals(1, exchange.getIn().getBody(Movie.class).getRatings().size());
    }

    @Test
    public void should_process_valid_unrated_movie() throws Exception {

        exchange = createExchangeWithBody(createCamelContext(), UNRATED_MOVIE);

        messageProcessor.process(exchange);

        assertNotNull(exchange.getIn().getBody(Movie.class));
        assertEquals("Deep Blue Sea", exchange.getProperty(MOVIE_TITLE));
        assertEquals(0, exchange.getProperty(NUM_RATINGS));
        assertEquals(0, exchange.getIn().getBody(Movie.class).getRatings().size());
    }

    @Test
    public void should_process_valid_movie_with_multiple_ratings() throws Exception {

        exchange = createExchangeWithBody(createCamelContext(), MOVIE_WITH_MULTIPLE_RATINGS);

        messageProcessor.process(exchange);

        assertNotNull(exchange.getIn().getBody(Movie.class));
        assertEquals("Superman", exchange.getProperty(MOVIE_TITLE));
        assertEquals(3, exchange.getProperty(NUM_RATINGS));
        assertEquals(3, exchange.getIn().getBody(Movie.class).getRatings().size());
    }

    @Test(expected = SplitterException.class)
    public void should_not_process_invalid_movie_message() throws Exception {

        String invalidInput = "{\"hello\": \"world\"}";

        exchange = createExchangeWithBody(createCamelContext(), invalidInput);

        messageProcessor.process(exchange);

        assertNull(exchange.getIn().getBody(Movie.class));
        assertNull(exchange.getProperty(MOVIE_TITLE));
        assertNull(exchange.getProperty(NUM_RATINGS));
    }

    @Test(expected = Exception.class)
    public void should_throw_exception_for_invalid_message_format() throws Exception {

        String invalidInput = "hello world!";

        exchange = createExchangeWithBody(createCamelContext(), invalidInput);

        messageProcessor.process(exchange);

        assertNull(exchange.getIn().getBody(Movie.class));
        assertNull(exchange.getProperty(MOVIE_TITLE));
        assertNull(exchange.getProperty(NUM_RATINGS));
    }
}