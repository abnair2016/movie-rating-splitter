package com.anair.demo.component.movieratingsplitter.service;

import com.anair.demo.component.movieratingsplitter.model.FilmRating;
import com.anair.demo.component.movieratingsplitter.model.Movie;
import com.anair.demo.component.movieratingsplitter.util.JSONUtil;
import com.google.common.collect.Lists;
import org.apache.camel.Exchange;
import org.apache.camel.test.junit4.ExchangeTestSupport;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.anair.demo.component.movieratingsplitter.util.TestFileUtils.getTestResourceAsString;
import static org.mockito.MockitoAnnotations.initMocks;

public class RatingsTransformerTest extends ExchangeTestSupport {

    private static final String MOVIE_WITH_MULTIPLE_RATINGS = getTestResourceAsString("test-json-files/movie-with-multiple-ratings.json");
    private static final String MOVIE_WITH_SINGLE_RATING = getTestResourceAsString("test-json-files/movie-with-single-rating.json");
    private static final String UNRATED_MOVIE = getTestResourceAsString("test-json-files/movie-with-no-rating.json");

    private RatingsTransformer ratingsTransformer;
    private Exchange exchange;

    @Before
    public void setUp() {
        initMocks(this);
        ratingsTransformer = new RatingsTransformer();
    }

    @Test
    public void should_transform_Movie_with_multiple_ratings_to_FilmRatings() throws Exception {

        Movie movieWithMultipleRatings = JSONUtil.fromJsonString(MOVIE_WITH_MULTIPLE_RATINGS, Movie.class);

        exchange = createExchangeWithBody(createCamelContext(), movieWithMultipleRatings);

        ratingsTransformer.process(exchange);

        List<FilmRating> filmRatings = convertToList((Iterator<String>) exchange.getIn().getBody());

        assertNotNull(filmRatings);
        assertEquals(3, filmRatings.size());
        assertNotNull(filmRatings.get(0).getFilm());
        assertEquals("SUPERMAN", filmRatings.get(0).getFilm().getTitle().toUpperCase());
        assertNotNull(filmRatings.get(1).getFilm());
        assertEquals("SUPERMAN", filmRatings.get(1).getFilm().getTitle().toUpperCase());
        assertNotNull(filmRatings.get(2).getFilm());
        assertEquals("SUPERMAN", filmRatings.get(2).getFilm().getTitle().toUpperCase());
    }

    @Test
    public void should_transform_Movie_with_single_rating_to_FilmRating() throws Exception {

        Movie movieWithSingleRating = JSONUtil.fromJsonString(MOVIE_WITH_SINGLE_RATING, Movie.class);

        exchange = createExchangeWithBody(createCamelContext(), movieWithSingleRating);

        ratingsTransformer.process(exchange);

        List<FilmRating> filmRatings = convertToList((Iterator<String>) exchange.getIn().getBody());

        assertNotNull(filmRatings);
        assertEquals(1, filmRatings.size());
        assertNotNull(filmRatings.get(0).getFilm());
        assertEquals("CROUCHING TIGER, HIDDEN DRAGON", filmRatings.get(0).getFilm().getTitle().toUpperCase());
        assertNotNull(filmRatings.get(0).getSource());
        assertEquals("INTERNET MOVIE DATABASE", filmRatings.get(0).getSource().toUpperCase());
        assertNotNull(filmRatings.get(0).getValue());
        assertEquals("7.9/10", filmRatings.get(0).getValue());
    }

    @Test
    public void should_transform_unrated_Movie_to_FilmRating() throws Exception {

        Movie unratedMovie = JSONUtil.fromJsonString(UNRATED_MOVIE, Movie.class);

        exchange = createExchangeWithBody(createCamelContext(), unratedMovie);

        ratingsTransformer.process(exchange);

        List<FilmRating> filmRatings = convertToList((Iterator<String>) exchange.getIn().getBody());

        assertNotNull(filmRatings);
        assertEquals(1, filmRatings.size());
        assertEquals("DEEP BLUE SEA", filmRatings.get(0).getFilm().getTitle().toUpperCase());
        assertNotNull(filmRatings.get(0).getSource());
        assertEquals("UNRATED", filmRatings.get(0).getSource().toUpperCase());
        assertNotNull(filmRatings.get(0).getValue());
        assertEquals("UNRATED", filmRatings.get(0).getValue().toUpperCase());
    }

    private List<FilmRating> convertToList(Iterator<String> filmRatingsIterator) {
        return Lists.newArrayList(filmRatingsIterator).stream().map(this::toFilmRating).collect(Collectors.toList());
    }

    private FilmRating toFilmRating(String filmRatingsAsString) {
        try {
            return JSONUtil.fromJsonString(filmRatingsAsString, FilmRating.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}