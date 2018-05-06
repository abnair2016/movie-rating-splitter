## Movie Rating Splitter Service

### What does this Service do?

The Movie Rating Splitter Service is a microservice that:
* Consumes from the movie-message Kafka topic
* Splits the Movie message by the ratings it has received to individual FilmRating messages
* Produces to the film-ratings-message Kafka topic


![Movie Rating Splitter Service](https://github.com/abnair2016/movie-rating-splitter/blob/master/movie-rating-splitter-service-overview-diagram.jpg)


### Prerequisites

In order to run locally you will need to have at least the following installed in your local development environment:

* IntelliJ Community Edition 2016.3.5 or above
* Maven 3.3.9 or above
* Minikube v0.17.1 or v0.21.0
* Kubernetes client version v1.5.3 or v1.7.0 
* Kubernetes server version v1.5.3 
* Java 1.8.0_XXX - Java8 openjdk or above
* Oracle VirtualBox 5.0.40 or 5.1.18

### How do I set this up to run it (successfully)?

1. Clone or download the project: `git clone https://github.com/abnair2016/movie-rating-splitter.git`
2. Navigate to movie-rating-splitter service root: `cd /path/to/movie-rating-splitter` and run the maven clean install command: `mvn -U clean install -DskipTests`
3. Navigate to the component-test folder: `cd /path/to/movie-rating-splitter/component-test` and run the `./minikube-startup.sh` script to install kafka and zookeeper images in your local minikube. After the script runs, it opens a minikube dashboard to show the status of the pods. Please wait until the pods are in a `Running` state.
4. Open the project in your IDE and add the following environment variables to be able to connect to kafka installed in your minikube:

        `KAFKA_ENDPOINT=192.168.99.100:30092`
        `READ_TOPIC=movie-message`
        `WRITE_TOPIC=film-ratings-message`
        `NAMESPACE=minikube`
        `GROUP_ID=movie-rating-splitter`

5. Run the Application
6. Run a POST command in your REST client to the following URL: `http://localhost:9999/producer` 
6. Use any of the Movie message JSON files available under the `/path/to/movie-rating-splitter/service/src/test/resources/test-json-files` folder as text to be pasted in the Body section as Payload
7. You can use KafkaTool to list the messages consumed and produced to the respective topics to verify the message was successfully produced and consumed respectively.

### How do I set up Kafka Tool?

1. Download the latest free version of Kafka Tool: http://www.kafkatool.com
2. Connect to the Kafka installed in your minikube at the following host and port: `192.168.99.100:30092`
3. Monitor and manage the topics using the GUI