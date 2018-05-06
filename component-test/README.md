## Movie Rating Splitter Component Test

### What does this Component Test do?

The Movie Rating Splitter Component test runs a Maven test that:
* Deploys pods and expose services on minikube for:
  * _Zookeeper + Kafka:_ To consume valid movie messages
* Runs the Movie Rating Splitter Service (SUT) as a separate Java Process
* Pushes a valid movie message onto the movie-message Kafka topic
* Splits the Movie message by ratings to individual FilmRating message
* Verifies that the actual SUT output(s) by consuming from the film-ratings-message Kafka topic and asserting the counts of split FilmRating messages against the ingested Movie message.

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

1. Get the latest version of the project from GitHub
2. Navigate to movie-rating-splitter service root: `cd /path/to/movie-rating-splitter` and run the maven clean install command: `mvn -U clean install -DskipTests`
3. Navigate to component-test directory: `cd /path/to/movie-rating-splitter/component-test`
4. Run the component test using command: `mvn test -P component`

### FAQ

#### My Minikube hangs or errors out saying "Insufficient memory"?

Run the `minikube delete` command to delete the minikube instance and re-run the component test using command: `mvn test -P component`. This should fix the memory issue.

#### I keep getting "LEADER NOT AVAILABLE" when running the test?

Potential resolutions have been documented here: https://stackoverflow.com/questions/45748536/kafka-inaccessible-once-inside-kubernetes-minikube/48856311#48856311

Please ensure you check the versions as listed below:
1. **Kubernetes Client and Server versions:** Use command: kubectl version. We have tested in kubernetes client versions v1.5.3 and v1.7.0 successfully and we force minikube to start on a supported compatible kubernetes server version v1.5.3. However, if your server version displays a version other than this, that could be the reason for the error.
2. **VirtualBox version:** We have tested in VirtualBox versions 5.0.40 and 5.1.18 successfully.
3. **Minikube version:** Use command: minikube version. We have tested in minikube versions v0.17.1 and v0.21.0 successfully.