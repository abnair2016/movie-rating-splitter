package com.anair.demo.component.movieratingsplitter.component;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Properties;

import static com.anair.demo.component.movieratingsplitter.component.FileUtil.getResourceAsString;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

@Category(ComponentTest.class)
public class MovieRatingsSplitterServiceComponentTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieRatingsSplitterServiceComponentTest.class);

    // Movie Ratings Splitter Monitor Request Paths
    private static final String SPLITTER_SERVICE_STATUS = "http://localhost:9999/monitoring/health";

    // Wait times
    private static final int THREE = 3;
    private static final int THIRTY = 30;
    private static final Long ONE_SECOND = 1000L;

    // Status of Movie Ratings Splitter Service
    private static final String UP = "UP";

    // Kafka Properties
    private static final String MOVIE_TOPIC = "movie-message";
    private static final String FILM_RATINGS_TOPIC = "film-ratings-message";

    @Test
    public void
    should_consume_movie_with_multiple_ratings() {

        MovieRatingsSplitterServiceProcess.startIfNotRunning("multiple-movie-ratings-scenario");

        LOGGER.info("===========================================================");
        LOGGER.info("Await until successful response received from Movie Ratings Splitter Service...");
        LOGGER.info("===========================================================");

        awaitUntilSuccessfulResponseReceivedFromMovieRatingsSplitterService();

        LOGGER.info("===========================================================");
        LOGGER.info("Await completed after receiving response from Movie Ratings Splitter Service.");
        LOGGER.info("===========================================================");

        LOGGER.info("===========================================================");
        LOGGER.info("Sending Movie message with multiple ratings to {} topic...", MOVIE_TOPIC);
        LOGGER.info("===========================================================");

        sendMessageToTopic(getResourceAsString("movie-with-multiple-ratings.json"));

        LOGGER.info("===========================================================");
        LOGGER.info("Sent movie message with multiple ratings to {} topic", MOVIE_TOPIC);
        LOGGER.info("===========================================================");

        LOGGER.info("===========================================================");
        LOGGER.info("Consuming ratings message from {} topic...", FILM_RATINGS_TOPIC);
        LOGGER.info("===========================================================");

        int actualMessageCount = messagesConsumedFromTopic("multiple-film-ratings-splitter", 3);
        assertEquals(3, actualMessageCount);

        LOGGER.info("===========================================================");
        LOGGER.info("Finished consuming ratings message from {} topic... Movie Ratings Splitter Test completed!", FILM_RATINGS_TOPIC);
        LOGGER.info("===========================================================");
    }

    @Test
    public void
    should_consume_movie_with_one_rating() {

        MovieRatingsSplitterServiceProcess.startIfNotRunning("single-movie-rating-scenario");

        LOGGER.info("===========================================================");
        LOGGER.info("Await until successful response received from Movie Ratings Splitter Service...");
        LOGGER.info("===========================================================");

        awaitUntilSuccessfulResponseReceivedFromMovieRatingsSplitterService();

        LOGGER.info("===========================================================");
        LOGGER.info("Await completed after receiving response from Movie Ratings Splitter Service.");
        LOGGER.info("===========================================================");

        LOGGER.info("===========================================================");
        LOGGER.info("Sending Movie message with single rating to {} topic...", MOVIE_TOPIC);
        LOGGER.info("===========================================================");

        sendMessageToTopic(getResourceAsString("movie-with-single-rating.json"));

        LOGGER.info("===========================================================");
        LOGGER.info("Sent movie message with single rating to {} topic", MOVIE_TOPIC);
        LOGGER.info("===========================================================");

        LOGGER.info("===========================================================");
        LOGGER.info("Consuming ratings message from {} topic...", FILM_RATINGS_TOPIC);
        LOGGER.info("===========================================================");

        int actualMessageCount = messagesConsumedFromTopic("single-film-rating-splitter", 1);
        assertEquals(1, actualMessageCount);

        LOGGER.info("===========================================================");
        LOGGER.info("Finished consuming ratings message from {} topic... Movie Ratings Splitter Test completed!", FILM_RATINGS_TOPIC);
        LOGGER.info("===========================================================");
    }

    @Test
    public void
    should_consume_movie_with_no_rating() {

        MovieRatingsSplitterServiceProcess.startIfNotRunning("unrated-movie-scenario");

        LOGGER.info("===========================================================");
        LOGGER.info("Await until successful response received from Movie Ratings Splitter Service...");
        LOGGER.info("===========================================================");

        awaitUntilSuccessfulResponseReceivedFromMovieRatingsSplitterService();

        LOGGER.info("===========================================================");
        LOGGER.info("Await completed after receiving response from Movie Ratings Splitter Service.");
        LOGGER.info("===========================================================");

        LOGGER.info("===========================================================");
        LOGGER.info("Sending Movie message with no rating to {} topic...", MOVIE_TOPIC);
        LOGGER.info("===========================================================");

        sendMessageToTopic(getResourceAsString("movie-with-no-rating.json"));

        LOGGER.info("===========================================================");
        LOGGER.info("Sent movie message with no rating to {} topic", MOVIE_TOPIC);
        LOGGER.info("===========================================================");

        LOGGER.info("===========================================================");
        LOGGER.info("Consuming ratings message from {} topic...", FILM_RATINGS_TOPIC);
        LOGGER.info("===========================================================");

        int actualMessageCount = messagesConsumedFromTopic("unrated-film-rating-splitter", 1);
        assertEquals(1, actualMessageCount);

        LOGGER.info("===========================================================");
        LOGGER.info("Finished consuming ratings message from {} topic... Movie Ratings Splitter Test completed!", FILM_RATINGS_TOPIC);
        LOGGER.info("===========================================================");
    }

    private void awaitUntilSuccessfulResponseReceivedFromMovieRatingsSplitterService() {

        await()
                .pollInterval(THIRTY, SECONDS)
                .atMost(THREE, MINUTES)
                .until(() -> {
                    try {
                        RestAssured.given()
                                .when()
                                .get(SPLITTER_SERVICE_STATUS)
                                .getStatusCode();
                    } catch (Exception exception) {
                        LOGGER.info("Movie Ratings Splitter Service is not ready yet! Reason: {}", exception.getMessage());
                        awaitUntilSuccessfulResponseReceivedFromMovieRatingsSplitterService();
                    }

                    Response response = RestAssured.given()
                            .when()
                            .get(SPLITTER_SERVICE_STATUS)
                            .then()
                            .extract().response();

                    LOGGER.info("Response JSON: {}", response.asString());

                    return response.getBody().asString().contains(UP);
                });
    }

    private void sendMessageToTopic(String messageAsString) {
        try (Producer<String, String> producer = new KafkaProducer<>(producerProperties())) {
            producer.send(new ProducerRecord<>(MOVIE_TOPIC, messageAsString));
        } catch (Exception e) {
            MovieRatingsSplitterServiceProcess.stopIfRunning();
            e.printStackTrace();
        }
    }

    private int messagesConsumedFromTopic(String consumerGroupIdForTest, int expectedMessageCount) {

        int actualMessageCount = 0;
        final int giveUp = 25;
        int noOfPolls = 0;

        try (final Consumer<String, String> consumer = new KafkaConsumer<>(consumerProperties(consumerGroupIdForTest))) {
            consumer.subscribe(Collections.singletonList(FILM_RATINGS_TOPIC));
            while (actualMessageCount < expectedMessageCount) {

                final ConsumerRecords<String, String> consumerRecords = consumer.poll(ONE_SECOND);

                actualMessageCount += consumerRecords.count();

                consumerRecords.forEach(record ->
                        LOGGER.info("Consumer Record:({}: {}: {}: {})\n",
                                record.partition(), record.offset(),
                                record.key(), record.value())
                );

                consumer.commitSync();

                noOfPolls++;
                LOGGER.info("Completed {} polls", noOfPolls);
                if (noOfPolls > giveUp) {
                    LOGGER.info("Completed {} polls... Giving up.", noOfPolls);
                    return actualMessageCount;
                }
            }
        } catch (Exception e) {
            MovieRatingsSplitterServiceProcess.stopIfRunning();
            e.printStackTrace();
        }
        return actualMessageCount;
    }

    private Properties producerProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.99.100:30092");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return props;
    }

    private Properties consumerProperties(String consumerGroupIdForTest) {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.99.100:30092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupIdForTest);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 60000);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 120000);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1048576000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return props;
    }

    @After
    public void tearDown() {
        MovieRatingsSplitterServiceProcess.stopIfRunning();
    }

}