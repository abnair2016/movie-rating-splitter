# Component Testing with Kubernetes using Minikube: A Developer's Approach

The adoption of microservices architectures, modular components requiring integration, an ever-growing need to be value-centric and delivering to business through CI / CD (continuous integration / continuous delivery), popularity of containerisation, orchestration and cloud computing, developers these days need to think beyond the realm of developing applications that are covered by unit tests alone. 

The requirement for a fail-fast feedback is becoming even more important, especially within an iterative development environment. 

It is important to know whether the microservice being developed interacts with external components and behaves as expected "as a component" much earlier in the delivery process as opposed to an anomaly being identified later in the process. i.e. once the microservice has already been developed and deployed and a behavioural anomaly caught during an end-to-end test. 

The motivation for this post is precisely to address this gap by using component tests that can run against the microservice under test (SUT) that purely tests the SUT as a component even before it is deployed to the higher environments i.e. At least run one happy path component test locally to ensure that any new feature development has not changed the expected behaviour of the component as a whole. 

**Given:** A valid input

**When:** The input is ingested into the component 

**Then:** Assert the expected outputs against the actual outputs of the component.

Below are the details on how I went about implementing a basic component test for the SUT. 

## Sample Project

To elucidate how I incorporated component testing within my workflow, please refer to the sample <a href="http://www.github.com/abnair2016/movie-rating-splitter" target="_blank">movie-rating-splitter</a> project.

![Movie Rating Splitter Service](https://github.com/abnair2016/movie-rating-splitter/blob/master/movie-rating-splitter-service-overview-diagram.jpg)

In this sample project, the `movie-rating-splitter` service (SUT) communicates with an external component i.e. Kafka.
 
It `consumes` a valid input `Movie` message from one Kafka topic named `movie-message`, transforms and splits the input message to `FilmRating` messages and `produces` to another Kafka topic named `film-ratings-message`. 
 
Using this component testing approach, the main objective is to isolate the `movie-rating-splitter` service from the external component that it communicates with (i.e. Kafka) and run a test that asserts the response of the SUT, given a specific (set of) input(s).

## What does this Component Test do?

The `movie-rating-splitter` component test runs a Maven test that:
* Deploys pods and exposes services on minikube for:
  * _Zookeeper + Kafka:_ To consume valid `Movie` message from the `movie-message` kafka topic
* Runs the `movie-rating-splitter` service (SUT), as a Java Process
* Pushes a valid `Movie` message onto the `movie-message` Kafka topic
* Pushes the response of the SUT i.e. `FilmRating` messages onto the `film-ratings-message` kafka topic
* Verifies that the actual SUT output(s) by consuming from the `film-ratings-message` Kafka topic and asserting the expected counts of split `FilmRating` messages against the actual messages produced by the SUT onto the `film-ratings-message` topic.

## Pre-requisites

In order to run this test locally, you will need to have at least the following installed in your local development environment:

* IntelliJ Community Edition 2016.3.5 or above
* Maven 3.3.9 or above
* Minikube v0.17.1 or v0.21.0
* Kubernetes client version v1.5.3 or v1.7.0 
* Kubernetes server version v1.5.3 
* Java 1.8.0_XXX - Java8 openjdk or above
* Oracle VirtualBox 5.0.40 or 5.1.18

## How do I set this up to run it (locally)?

1. Clone or download the project from GitHub: `git clone https://github.com/abnair2016/movie-rating-splitter.git`
2. Navigate to movie-rating-splitter service root: `cd /path/to/movie-rating-splitter` and run the maven clean install command: `mvn -U clean install -DskipTests`
3. Navigate to component-test directory: `cd /path/to/movie-rating-splitter/component-test`
4. Run the component test using command: `mvn test -P component`

## Preparation

The `component-test` module of the project has the following setups:

### 1. Minikube:

1. The minikube setup script `minikube-startup.sh` runs everytime a `mvn test -P component` command is issued which will:

    * Start minikube if not already running
    * Delete all existing deployments and services in minikube
    * Install afresh the pods and services required for the component test
    * Recreate the topics required for the component test

2. _**minikube-startup.sh**_

    ```
    #!/bin/bash
    
    delete_all_minikube_resources() {
       kubectl --context=minikube delete --all deployments
       kubectl --context=minikube delete --all services
    }
    
    create_deployment_and_service() {
        echo "Deploying ${1} pod"
        kubectl apply -f "$deploymentsDirectory/${1}-deployment.yml"
    
        echo "Deploying service for ${1}"
        kubectl apply -f "$servicesDirectory/${1}-service.yml"
    }
    
    start_minikube() {
        if [[ $(minikube status | grep -o "Running" 2>/dev/null | uniq) != "Running" ]]; then
            minikube start --cpus 2 --memory 8192 --kubernetes-version v1.5.3
            # Wait for cluster to start
            echo "Waiting for cluster to start"
            while [ "$ready" != "Running" ]; do
                ready=$(minikube status | grep localkube | grep -o "Running")
                sleep 10
            done
        fi
    }
    
    setup_topic() {
        echo "Setting up kafka topic - started"
        pod_name=$(kubectl get pods | awk '/kafka/{print$1}')
    
        echo "/opt/kafka/bin/kafka-topics.sh --zookeeper 192.168.99.100:30181 --create --topic movie-message --partitions 1 --replication-factor 1" | kubectl exec "$pod_name" -it bash
        echo "/opt/kafka/bin/kafka-topics.sh --zookeeper 192.168.99.100:30181 --create --topic film-ratings-message --partitions 1 --replication-factor 1" | kubectl exec "$pod_name" -it bash
        echo "==========================================================="
        echo "List of available topics"
        echo "==========================================================="
        echo "/opt/kafka/bin/kafka-topics.sh --zookeeper 192.168.99.100:30181 --list" | kubectl exec "$pod_name" -it bash
        echo "Setting up kafka topic - completed"
    }
    
    echo "==========================================================="
    echo "Minikube setup started..."
    echo "==========================================================="
    # Switch context to minikube
    echo "Switching context to minikube"
    kubectl config use-context minikube
    
    # Kubernetes Variables
    kubernetesDirectory="kubernetes"
    servicesDirectory="$kubernetesDirectory/services"
    deploymentsDirectory="$kubernetesDirectory/deployments"
    initial_deployments=$( printf "zookeeper" | sort )
    initial_deployments_names=$( printf "zookeeper" )
    
    echo "${1}"
    
    # Start minikube if not already running
    start_minikube
    
    # Clean up minikube before starting the tests
    echo "Deleting all existing minikube resources"
    delete_all_minikube_resources
    
    # Pod / service deployments
    for i in $(printf "$initial_deployments"); do
        create_deployment_and_service "${i}"
    done
    
    echo "Waiting for initial deployments to be ready..."
    while [ "$ready" != "$initial_deployments_names" ]; do
        sleep 5
        ready=$(kubectl get deployments | awk '{if ($5==1) print $1 }' | sort)
        printf "%0.s-" {1..1}
    done
    
    echo "Setup of initial deployments - complete"
    
    # Set up kafka
    create_deployment_and_service "kafka"
    
    echo "Waiting for kafka to be ready..."
    while [ "$ready" != "started (kafka.server.KafkaServer)" ]; do
        sleep 5
        ready=$(kubectl logs `kubectl get pods | awk '/kafka/{print$1}'` | grep -o "started (kafka.server.KafkaServer)")
        printf "%0.s-" {1..1}
    done
    
    # Set up Kafka Topic
    echo "Setting up kafka topic..."
    setup_topic
    
    # Set docker env
    echo "Setting docker env..."
    eval $(minikube docker-env)
    
    # Open minikube dashboard
    echo "Opening minikube dashboard..."
    minikube dashboard &
    
    echo "==========================================================="
    echo "Minikube setup completed."
    echo "==========================================================="
    
    exit 0
    ```

### 2. Zookeeper:

1. Spins up a zookeeper pod in minikube using an image available on the internet (image used: `digitalwonderland/zookeeper`)

2. _zookeeper-deployment.yml_

    ```
    apiVersion: extensions/v1beta1
    kind: Deployment
    metadata:
      labels:
        app: zookeeper
      name: zookeeper
    spec:
      replicas: 1
      template:
        metadata:
          labels:
            app: zookeeper
        spec:
          containers:
          - name: zookeeper
            image: digitalwonderland/zookeeper
            imagePullPolicy: IfNotPresent
            ports:
            - containerPort: 2181
            resources:
              limits:
                cpu: 1
                memory: 1Gi
              requests:
                cpu: 200m
                memory: 400Mi
    ```

3. _zookeeper-service.yml_

    ```
    apiVersion: v1
    kind: Service
    metadata:
      name: zookeeper-service
      labels:
        app: zookeeper-service
    spec:
      ports:
      - port: 2181
        name: zookeeper-port
        targetPort: 2181
        nodePort: 30181
        protocol: TCP
      selector:
        app: zookeeper
      type: NodePort
    ```

4. Exposes the Zookeeper service on port number `30181`

### 3. Kafka:

1. Spins up a Kafka pod in minikube using an image available on the internet (image used: `wurstmeister/kafka`)

2. _kafka-deployment.yml_

    ```
    apiVersion: extensions/v1beta1
    kind: Deployment
    metadata:
      labels:
        app: kafka
      name: kafka
    spec:
      replicas: 1
      template:
        metadata:
          labels:
            app: kafka
        spec:
          containers:
          - name: kafka
            image: wurstmeister/kafka
            imagePullPolicy: IfNotPresent
            ports:
            - containerPort: 9092
            env:
            - name: KAFKA_ADVERTISED_HOST_NAME
              value: "192.168.99.100"
            - name: KAFKA_ADVERTISED_PORT
              value: "30092"
            - name: KAFKA_ZOOKEEPER_CONNECT
              value: 192.168.99.100:30181
            - name: KAFKA_BROKER_ID
              value: "999"
            - name: KAFKA_HEAP_OPTS
              value: "-Xmx1G -Xms1G"
            - name: NAMESPACE
              value: "minikube"
            resources:
              limits:
                cpu: 1
                memory: 1024Mi
              requests:
                cpu: 500m
                memory: 1024Mi
    ```

3. _kafka-service.yml_

    ```
    apiVersion: v1
    kind: Service
    metadata:
      name: kafka-service
      labels:
        app: kafka
    spec:
      ports:
      - port: 9092
        name: kafka-port
        targetPort: 9092
        nodePort: 30092
        protocol: TCP
      selector:
        app: kafka
      type: NodePort
    ```

4. Exposes the Kafka service on port number `30092`

## SUT as a Java Process

```
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MovieRatingsSplitterServiceProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieRatingsSplitterServiceProcess.class);
    private static final String READ_TOPIC = "movie-message";
    private static final String WRITE_TOPIC = "film-ratings-message";

    private static Process process;
    private String jarFilePath = "../service/target";
    private String regexFile = "movie-rating-splitter-service-\\d+\\.\\d+\\.\\d+-SNAPSHOT\\.jar";
    private final Map<String, String> environmentVars = new HashMap<String, String>() {
        put("KAFKA_SECURITY_PROTOCOL", "PLAINTEXT");
        put("KAFKA_SESSION_TIMEOUT_MS", "120000");
        put("KAFKA_REQUEST_TIMEOUT_MS", "180000");
        put("KAFKA_POLL_TIMEOUT_MS", "1000");
        put("KAFKA_MAX_PARTITION_FETCH_BYTES", "1048576000");
        put("KAFKA_ACKS", "all");
        put("KAFKA_RETRIES", "0");
        put("KAFKA_BATCH_SIZE", "16384");
        put("KAFKA_LINGER_MS", "1");
        put("KAFKA_BUFFER", "33554432");
        put("KAFKA_MAX_REQUEST_SIZE", "1048576000");
        put("KAFKA_ENDPOINT", "192.168.99.100:30092");
        put("NAMESPACE","minikube");
        put("READ_TOPIC",READ_TOPIC);
        put("WRITE_TOPIC",WRITE_TOPIC);
    };

    private static MovieRatingsSplitterServiceProcess getInstance() {
        return new MovieRatingsSplitterServiceProcess();
    }

    public static void startIfNotRunning(String consumerGroupIdForSplitterService) {
        // check if it's not running and if not start
        getInstance().start(consumerGroupIdForSplitterService);
    }

    public static void stopIfRunning() {
        process.destroy();
        try {
            if (!process.waitFor(1, TimeUnit.NANOSECONDS)) {
                process.destroyForcibly();
                LOGGER.info("=========================================");
                LOGGER.info("Stopped Movie Ratings Splitter Service ");
                LOGGER.info("=========================================");
            }
        } catch (InterruptedException e) {
            throw new UncheckedExecutionException(e);
        }
    }

    private void start(String consumerGroupIdForSplitterService) {
        Thread thread = new Thread(() -> {
            try {
                String processLog;
                File dir = new File(jarFilePath);
                FileFilter fileFilter = new RegexFileFilter(regexFile);
                File[] filesArray = dir.listFiles(fileFilter);

                if (filesArray != null && filesArray.length > 0) {
                    List<File> files = Arrays.asList(filesArray);
                    for (File file : files) {
                        jarFilePath = file.getAbsolutePath();
                    }
                }

                String command = "java -jar " + jarFilePath;


                LOGGER.info("Started Movie Ratings Splitter Service using command: {} with group Id: {}", command, consumerGroupIdForSplitterService);
                process = Runtime.getRuntime().exec(command, getEnvironmentVars(consumerGroupIdForSplitterService));

                try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    // read the output from the command
                    while ((processLog = stdInput.readLine()) != null) {
                        LOGGER.info(processLog);
                    }
                }

                try (BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    // read any errors from the attempted command
                    while ((processLog = stdError.readLine()) != null) {
                        LOGGER.info(processLog);
                    }
                }

            } catch (IOException ioException) {
                LOGGER.error("Exception occurred! Here's what I know: {}", ioException.getMessage());
                ioException.printStackTrace();
            }

        });

        thread.start();
    }

    private String[] getEnvironmentVars(String consumerGroupIdForSplitterService) {
        ImmutableMap<String, String> properties = ImmutableMap.of("GROUP_ID", consumerGroupIdForSplitterService);
        environmentVars.putAll(properties);
        return environmentVars.entrySet().stream().map(e -> String.format("%s=%s", e.getKey(), e.getValue())).collect(Collectors.toList()).toArray(new String[]{});
    }
}
```

## Component Tests

```
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

        MovieRatingsSplitterServiceProcess.stopIfRunning();
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

        MovieRatingsSplitterServiceProcess.stopIfRunning();
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

        MovieRatingsSplitterServiceProcess.stopIfRunning();
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
```

## Summary

The `movie-rating-splitter` service component tests perform the following steps:
* Switches to `minikube` context
* Starts up minikube if it is not running already
* Deletes all existing deployments and services currently running on minikube
* Creates the below listed initial deployments and exposes their respective services:
    * `zookeeper` to interact with `kafka`
    * Once the `zookeeper` deployment is running, creates the `kafka` deployment and service
* Opens the minikube dashboard to give the user a visual representation of the running minikube pods and services that were created
* Starts the actual `movie-rating-splitter` microservice as a Java process 
* Runs the Component Tests

## F.A.Q.

### 1. My Minikube hangs or errors out saying "Insufficient memory"?

Run the `minikube delete` command to delete the minikube instance and re-run the component test using command: `mvn test -P component`. This should fix the memory issue.

### 2. I keep getting `LEADER NOT AVAILABLE` when running the test?

Potential resolutions have been documented in <a href="https://stackoverflow.com/questions/45748536/kafka-inaccessible-once-inside-kubernetes-minikube/48856311#48856311" target="_blank">Stack Overflow</a>.

Please ensure you check the versions as listed below:
1. **Kubernetes Client and Server versions:** Use command: kubectl version. We have tested in kubernetes client versions v1.5.3 and v1.7.0 successfully and we force minikube to start on a supported compatible kubernetes server version v1.5.3. However, if your server version displays a version other than this, that could be the reason for the error.
2. **VirtualBox version:** We have tested in VirtualBox versions 5.0.40 and 5.1.18 successfully.
3. **Minikube version:** Use command: minikube version. We have tested in minikube versions v0.17.1 and v0.21.0 successfully.
