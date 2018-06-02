package com.anair.demo.component.movieratingsplitter.component;

import com.anair.demo.component.movieratingsplitter.component.model.FilmRating;
import com.anair.demo.component.movieratingsplitter.component.util.JSONUtil;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.rule.OutputCapture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.anair.demo.component.movieratingsplitter.component.util.FileUtil.getResourceAsString;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Category(ComponentTest.class)
public class MovieRatingsSplitterServiceComponentTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieRatingsSplitterServiceComponentTest.class);

    // Movie Ratings Splitter Monitor Request Paths
    private static final String SPLITTER_SERVICE_STATUS = "http://localhost:9999/monitoring/health";

    // Wait times
    private static final int THREE = 3;
    private static final int THIRTY = 30;
    private static final long ONE_SECOND = 1000L;
    private static final long FIVE_SECONDS = 5000L;
    private static final long THIRTY_SECONDS = 30000L;

    // Status of Movie Ratings Splitter Service
    private static final String UP = "UP";

    // Kafka Properties
    private static final String READ_TOPIC = "movie-message";
    private static final String WRITE_TOPIC = "film-ratings-message";

    // Consumer groups for SUT and Component Test
    private static final String SUT_CONSUMER_GROUP_ID = "sut-consumer-group-id";
    private static final String COMPONENT_TEST_CONSUMER_GROUP_ID = "component-test-consumer-group-id";

    // JSON Payload Resource File Names
    private static final String MOVIE_WITH_MULTIPLE_RATINGS_JSON = "movie-with-multiple-ratings.json";
    private static final String MOVIE_WITH_SINGLE_RATING_JSON = "movie-with-single-rating.json";
    private static final String MOVIE_WITH_NO_RATING_JSON = "movie-with-no-rating.json";
    private static final String INVALID_MESSAGE_JSON = "invalid-input-message.json";

    // Kafka Consumer and Producer
    private static Consumer<String, String> consumer;
    private static Producer<String, String> producer;

    @Rule
    public final OutputCapture outputCapture = new OutputCapture();

    @BeforeClass
    public static void preSetup() throws InterruptedException {

        // Set up Kafka Consumer
        consumer = new KafkaConsumer<>(consumerProperties(COMPONENT_TEST_CONSUMER_GROUP_ID));
        // Subscribe the Kafka Consumer to film-ratings-message topic
        consumer.subscribe(Collections.singletonList(WRITE_TOPIC));
        // Set up Kafka Producer
        producer = new KafkaProducer<>(producerProperties());

        // Start the SUT process
        MovieRatingsSplitterServiceProcess.startIfNotRunning(SUT_CONSUMER_GROUP_ID);

        LOGGER.info("===========================================================");
        LOGGER.info("Waiting for Kafka Consumer to be ready...");
        LOGGER.info("===========================================================");

        Thread.sleep(THIRTY_SECONDS);

        LOGGER.info("===========================================================");
        LOGGER.info("Waiting for Kafka Consumer to be ready - Completed");
        LOGGER.info("===========================================================");

    }

    @Before
    public void setUp() {
        LOGGER.info("===========================================================");
        LOGGER.info("Await until successful response received from Movie Ratings Splitter Service...");
        LOGGER.info("===========================================================");

        awaitUntilSuccessfulResponseReceivedFromMovieRatingsSplitterService();

        LOGGER.info("===========================================================");
        LOGGER.info("Await completed after receiving response from Movie Ratings Splitter Service.");
        LOGGER.info("===========================================================");
    }

    @Test
    public void
    should_consume_movie_with_multiple_ratings() {

        LOGGER.info("===========================================================");
        LOGGER.info("Sending Movie message with multiple ratings to {} topic...", READ_TOPIC);
        LOGGER.info("===========================================================");

        sendMessageToTopic(getResourceAsString(MOVIE_WITH_MULTIPLE_RATINGS_JSON));

        LOGGER.info("===========================================================");
        LOGGER.info("Sent movie message with multiple ratings to {} topic", READ_TOPIC);
        LOGGER.info("===========================================================");

        LOGGER.info("===========================================================");
        LOGGER.info("Consuming ratings message from {} topic...", WRITE_TOPIC);
        LOGGER.info("===========================================================");

        List<String> actualMessages = messagesConsumedFromTopic(3);
        assertNotNull(actualMessages);

        List<FilmRating> filmRatings = transformToFilmRatings(actualMessages);

        assertNotNull(filmRatings);
        assertEquals(3, filmRatings.size());

        assertNotNull(filmRatings.get(0).getFilm());
        assertEquals("Superman", filmRatings.get(0).getFilm().getTitle());

        assertNotNull(filmRatings.get(1).getFilm());
        assertEquals("Superman", filmRatings.get(1).getFilm().getTitle());

        assertNotNull(filmRatings.get(2).getFilm());
        assertEquals("Superman", filmRatings.get(2).getFilm().getTitle());

        LOGGER.info("===========================================================");
        LOGGER.info("Finished consuming ratings message from {} topic... Movie Ratings Splitter Test completed!", WRITE_TOPIC);
        LOGGER.info("===========================================================");
    }

    @Test
    public void
    should_consume_movie_with_one_rating() {

        LOGGER.info("===========================================================");
        LOGGER.info("Sending Movie message with single rating to {} topic...", READ_TOPIC);
        LOGGER.info("===========================================================");

        sendMessageToTopic(getResourceAsString(MOVIE_WITH_SINGLE_RATING_JSON));

        LOGGER.info("===========================================================");
        LOGGER.info("Sent movie message with single rating to {} topic", READ_TOPIC);
        LOGGER.info("===========================================================");

        LOGGER.info("===========================================================");
        LOGGER.info("Consuming ratings message from {} topic...", WRITE_TOPIC);
        LOGGER.info("===========================================================");

        List<String> actualMessages = messagesConsumedFromTopic(1);
        assertNotNull(actualMessages);

        List<FilmRating> filmRatings = transformToFilmRatings(actualMessages);

        assertNotNull(filmRatings);
        assertEquals(1, filmRatings.size());

        assertNotNull(filmRatings.get(0).getFilm());
        assertEquals("Crouching Tiger, Hidden Dragon", filmRatings.get(0).getFilm().getTitle());
        assertEquals("Internet Movie Database", filmRatings.get(0).getSource());
        assertEquals("7.9/10", filmRatings.get(0).getValue());

        LOGGER.info("===========================================================");
        LOGGER.info("Finished consuming ratings message from {} topic... Movie Ratings Splitter Test completed!", WRITE_TOPIC);
        LOGGER.info("===========================================================");
    }

    @Test
    public void
    should_consume_movie_with_no_rating() {

        LOGGER.info("===========================================================");
        LOGGER.info("Sending Movie message with no rating to {} topic...", READ_TOPIC);
        LOGGER.info("===========================================================");

        sendMessageToTopic(getResourceAsString(MOVIE_WITH_NO_RATING_JSON));

        LOGGER.info("===========================================================");
        LOGGER.info("Sent movie message with no rating to {} topic", READ_TOPIC);
        LOGGER.info("===========================================================");

        LOGGER.info("===========================================================");
        LOGGER.info("Consuming ratings message from {} topic...", WRITE_TOPIC);
        LOGGER.info("===========================================================");

        List<String> actualMessages = messagesConsumedFromTopic(1);
        assertNotNull(actualMessages);

        List<FilmRating> filmRatings = transformToFilmRatings(actualMessages);

        assertNotNull(filmRatings);
        assertEquals(1, filmRatings.size());

        assertNotNull(filmRatings.get(0).getFilm());
        assertEquals("Deep Blue Sea", filmRatings.get(0).getFilm().getTitle());
        assertEquals("Unrated", filmRatings.get(0).getSource());
        assertEquals("Unrated", filmRatings.get(0).getValue());

        LOGGER.info("===========================================================");
        LOGGER.info("Finished consuming ratings message from {} topic... Movie Ratings Splitter Test completed!", WRITE_TOPIC);
        LOGGER.info("===========================================================");
    }

    @Test
    public void
    should_not_consume_invalid_message() throws InterruptedException {

        LOGGER.info("===========================================================");
        LOGGER.info("Sending invalid message to {} topic...", READ_TOPIC);
        LOGGER.info("===========================================================");

        outputCapture.flush();
        sendMessageToTopic(getResourceAsString(INVALID_MESSAGE_JSON));
        Thread.sleep(FIVE_SECONDS);

        LOGGER.info("===========================================================");
        LOGGER.info("Sent invalid message to {} topic", READ_TOPIC);
        LOGGER.info("===========================================================");

        outputCapture.expect(containsString("Input message was not of expected Movie format or was empty"));

        List<String> actualMessages = messagesConsumedFromTopic(0);
        assertNotNull(actualMessages);
        assertEquals(0, actualMessages.size());

        LOGGER.info("===========================================================");
        LOGGER.info("Finished NOT consuming invalid message test... Movie Ratings Splitter Test completed!", WRITE_TOPIC);
        LOGGER.info("===========================================================");
    }

    private List<FilmRating> transformToFilmRatings(final List<String> actualMessages) {
        return actualMessages.stream()
                .map(this::toFilmRating)
                .collect(Collectors.toList());
    }

    private FilmRating toFilmRating(final String filmRatingAsString) {
        try {
            return JSONUtil.fromJsonString(filmRatingAsString, FilmRating.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        try {
            producer.send(new ProducerRecord<>(READ_TOPIC, messageAsString));
        } catch (Exception e) {
            MovieRatingsSplitterServiceProcess.stopIfRunning();
            e.printStackTrace();
        }
    }

    private List<String> messagesConsumedFromTopic(int expectedMessageCount) {

        int actualMessageCount = 0;
        final int giveUp = 25;
        int noOfPolls = 0;
        List<String> actualRecords = new ArrayList<>();

        try {
            while (actualMessageCount < expectedMessageCount) {

                final ConsumerRecords<String, String> consumerRecords = consumer.poll(ONE_SECOND);

                actualMessageCount += consumerRecords.count();

                if (consumerRecords.iterator().hasNext()) {

                    // We can safely get the first TopicPartition from the set as we always create a topic with only 1 partition
                    TopicPartition componentTestTopicPartition = consumerRecords.partitions().iterator().next();
                    actualRecords = consumerRecords.records(componentTestTopicPartition)
                            .stream()
                            .map(ConsumerRecord::value)
                            .collect(Collectors.toList());
                    LOGGER.info("Consumed [{}] message(s)", actualRecords.size());

                }

                consumer.commitSync();

                noOfPolls++;
                LOGGER.info("Consumed [{}] message(s)", actualRecords.size());
                LOGGER.info("Completed {} polls", noOfPolls);
                if (noOfPolls > giveUp) {
                    LOGGER.info("Completed {} polls... Giving up.", noOfPolls);
                    return actualRecords;
                }
            }
        } catch (Exception e) {
            MovieRatingsSplitterServiceProcess.stopIfRunning();
            e.printStackTrace();
        }
        return actualRecords;
    }

    private static Properties producerProperties() {
        final Properties props = new Properties();
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

    private static Properties consumerProperties(String consumerGroupId) {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.99.100:30092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 60000);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 120000);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1048576000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return props;
    }

    @AfterClass
    public static void postTearDown() {
        consumer.close();
        producer.close();
        MovieRatingsSplitterServiceProcess.stopIfRunning();
    }

}