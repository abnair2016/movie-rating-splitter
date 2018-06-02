package com.anair.demo.component.movieratingsplitter.util;

public final class SplitterConstants {

    //Route IDs and Endpoints
    public static final String START_ID = "start-id";
    public static final String START = "direct:startRoute";
    public static final String PROCESS_MESSAGE_ID = "process-message-id";
    public static final String PROCESS_MESSAGE = "direct:processMessage";
    public static final String TRANSFORM_TO_FILM_RATINGS_ID = "transform-to-film-ratings-id";
    public static final String TRANSFORM_TO_FILM_RATINGS = "direct:transformToFilmRatings";
    public static final String SPLIT_AND_WRITE_TO_KAFKA_TOPIC_ID = "split-and-write-to-kafka-topic-route-id";
    public static final String SPLIT_AND_WRITE_TO_KAFKA_TOPIC = "direct:splitAndWriteToKafka";

    //Properties
    public static final String MOVIE_TITLE = "movieTitle";
    public static final String NUM_RATINGS = "numberOfRatings";

    //Camel Kafka Config
    public static final String BROKERS = "brokers";
    public static final String SECURITY_PROTOCOL = "securityProtocol";
    public static final String TOPIC = "topic";
    public static final String REQUEST_REQUIRED_ACKS = "requestRequiredAcks";
    public static final String RETRIES = "retries";
    public static final String PRODUCER_BATCH_SIZE = "producerBatchSize";
    public static final String LINGER_MS = "lingerMs";
    public static final String BUFFER_MEMORY_SIZE = "bufferMemorySize";
    public static final String MAX_REQUEST_SIZE = "maxRequestSize";
    public static final String KEY_SERIALIZER_CLASS = "keySerializerClass";
    public static final String SERIALIZER_CLASS = "serializerClass";
    public static final String GROUP_ID = "groupId";
    public static final String SESSION_TIMEOUT_MS = "sessionTimeoutMs";
    public static final String CONSUMER_REQUEST_TIMEOUT_MS = "consumerRequestTimeoutMs";
    public static final String MAX_PARTITION_FETCH_BYTES = "maxPartitionFetchBytes";
    public static final String KEY_DESERIALIZER = "keyDeserializer";
    public static final String VALUE_DESERIALIZER = "valueDeserializer";

    //Exceptions
    public static final String EXCEPTION_MESSAGE = "${exception.message}";
    public static final String EXCEPTION_STACKTRACE = "${exception.stacktrace}";

    private SplitterConstants() {
    }
}
