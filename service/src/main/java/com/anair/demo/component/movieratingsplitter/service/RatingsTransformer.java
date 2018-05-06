package com.anair.demo.component.movieratingsplitter.service;

import com.anair.demo.component.movieratingsplitter.model.Film;
import com.anair.demo.component.movieratingsplitter.model.FilmRating;
import com.anair.demo.component.movieratingsplitter.model.Movie;
import com.anair.demo.component.movieratingsplitter.util.JSONUtil;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Iterator;

import static com.anair.demo.component.movieratingsplitter.util.SplitterConstants.MOVIE_TITLE;
import static com.anair.demo.component.movieratingsplitter.util.SplitterConstants.NUM_RATINGS;
import static com.anair.demo.component.movieratingsplitter.util.SplitterHelper.isEmpty;

@Component
public class RatingsTransformer implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RatingsTransformer.class);

    @Override
    public void process(final Exchange exchange) {

        Movie movie = exchange.getIn().getBody(Movie.class);

        LOGGER.info("Received Movie titled [{}] with [{}] movie ratings",
                        exchange.getProperty(MOVIE_TITLE, String.class),
                        exchange.getProperty(NUM_RATINGS, Integer.class));

        if (isEmpty(movie.getRatings())) {
            exchange.getIn().setBody( Collections.singletonList(createUnratedRatings(movie)).iterator());

        } else {

            Iterator<String> ratingsIterator = movie.getRatings().stream()
                    .map(ratings -> new FilmRating(ratings.getSource(), ratings.getValue(), new Film(movie)))
                    .map(filmRating -> {
                        try {
                            return JSONUtil.toJsonString(filmRating);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }).iterator();

            exchange.getIn().setBody(ratingsIterator);
        }
    }

    private String createUnratedRatings(Movie movie) {
        try {
            return JSONUtil.toJsonString(new FilmRating("Unrated", "Unrated", new Film(movie)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}