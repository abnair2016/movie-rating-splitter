package com.anair.demo.component.movieratingsplitter.service;

import com.anair.demo.component.movieratingsplitter.exception.SplitterException;
import com.anair.demo.component.movieratingsplitter.model.Movie;
import com.anair.demo.component.movieratingsplitter.util.JSONUtil;
import com.anair.demo.component.movieratingsplitter.util.SplitterConstants;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;

import static com.anair.demo.component.movieratingsplitter.util.SplitterHelper.isNotEmpty;

@Component
public class MessageProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProcessor.class);

    @Override
    public void process(final Exchange exchange) {

        final String inputMessageAsString = exchange.getIn().getBody(String.class);
        try {
            Movie movie = JSONUtil.fromJsonString(inputMessageAsString, Movie.class);

            if (isNotEmpty(movie) && !movie.equals(new Movie())) {

                Integer numberOfRatings = isNotEmpty(movie.getRatings())? movie.getRatings().size(): 0;
                LOGGER.info("Received movie title: [{}] with [{}] movie ratings", movie.getTitle(), numberOfRatings);
                exchange.setProperty(SplitterConstants.MOVIE_TITLE, movie.getTitle());
                exchange.setProperty(SplitterConstants.NUM_RATINGS, numberOfRatings);
                exchange.getIn().setBody(movie, Movie.class);
            } else {
                throw new SplitterException(String.format("Input message was not of expected %s format or was empty", Movie.class.getSimpleName()));
            }

        } catch (IOException e) {
            LOGGER.error("Input message was not of expected format: [{}]", Movie.class.getSimpleName());
            throw new UncheckedIOException(e);
        }

    }
}