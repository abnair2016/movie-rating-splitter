package com.anair.demo.component.movieratingsplitter.route;

import com.anair.demo.component.movieratingsplitter.config.SplitterConfig;
import com.anair.demo.component.movieratingsplitter.service.MessageProcessor;
import com.anair.demo.component.movieratingsplitter.service.RatingsTransformer;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.anair.demo.component.movieratingsplitter.util.SplitterConstants.*;
import static com.anair.demo.component.movieratingsplitter.util.SplitterHelper.buildURIParamsUsing;

@Component
public class SplitterRoute extends ExceptionHandlingRoute {

    private static final String CLASS_NAME = SplitterRoute.class.getSimpleName();

    private final SplitterConfig splitterConfig;
    private final MessageProcessor messageProcessor;
    private final RatingsTransformer ratingsTransformer;
    private final String readTopic;
    private final String writeTopic;
    private final String fromKafkaTopic;
    private final String toKafkaTopic;

    @Autowired
    public SplitterRoute(final SplitterConfig splitterConfig,
                         final MessageProcessor messageProcessor,
                         final RatingsTransformer ratingsTransformer) {
        this.splitterConfig = splitterConfig;
        this.messageProcessor = messageProcessor;
        this.ratingsTransformer = ratingsTransformer;
        this.readTopic = splitterConfig.getConsumerTopic();
        this.writeTopic = splitterConfig.getProducerTopic();
        this.fromKafkaTopic = "kafka:" + readTopic + "?" + buildURIParamsUsing(splitterConfig.getConsumerConfig());
        this.toKafkaTopic = "kafka:" + writeTopic + "?" + buildURIParamsUsing(splitterConfig.getProducerConfig());
    }

    @Override
    public void configure() {

        from(fromKafkaTopic)
                .log(LoggingLevel.INFO, "Consumed input message from " + readTopic)
                .to(START);

        from(START)
                .routeDescription("High level route summary")
                .to(PROCESS_MESSAGE)
                .to(TRANSFORM_TO_FILM_RATINGS)
                .to(SPLIT_AND_WRITE_TO_KAFKA_TOPIC)
                .end();

        from(PROCESS_MESSAGE)
                .routeId(PROCESS_MESSAGE_ID)
                .process(messageProcessor)
                .log(LoggingLevel.INFO, CLASS_NAME, "Movie Message with title ${property.movieTitle} received with ${property.numberOfRatings} ratings");

        from(TRANSFORM_TO_FILM_RATINGS)
                .routeId(TRANSFORM_TO_FILM_RATINGS_ID)
                .process(ratingsTransformer)
                .log(LoggingLevel.INFO, "Transformed Movie message to aggregated Film Ratings");

        from(SPLIT_AND_WRITE_TO_KAFKA_TOPIC)
                .routeId(SPLIT_AND_WRITE_TO_KAFKA_TOPIC_ID)
                .split(body())
                .shareUnitOfWork()
                .parallelProcessing()
                .log(LoggingLevel.INFO, "Splitted to Film Ratings messages")
                .removeHeaders("*")
                .to(toKafkaTopic)
                .log(LoggingLevel.INFO, "Sent Film Ratings message to " + writeTopic);

    }
}
