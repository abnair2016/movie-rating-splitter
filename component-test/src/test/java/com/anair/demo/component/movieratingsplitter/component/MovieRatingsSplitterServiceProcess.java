package com.anair.demo.component.movieratingsplitter.component;

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
    private final Map<String, String> environmentVars = new HashMap<String, String>() {{
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

    }};

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