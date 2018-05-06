package com.anair.demo.component.movieratingsplitter.controller;

import com.anair.demo.component.movieratingsplitter.config.SplitterConfig;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.anair.demo.component.movieratingsplitter.util.SplitterHelper.buildURIParamsUsing;

@RestController
public class RestMessageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestMessageController.class);

    private SplitterConfig splitterConfig;

    @EndpointInject
    private ProducerTemplate producerTemplate;

    @Autowired
    public RestMessageController(final SplitterConfig splitterConfig) {
        this.splitterConfig = splitterConfig;
    }

    @ApiOperation(value = "Sends a movie message provided as a JSON payload in the request body to the movie-message kafka topic", response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully produced to movie-message Kafka topic"),
            @ApiResponse(code = 401, message = "Failed to produce message to Kafka topic"),
            @ApiResponse(code = 403, message = "Failed to produce message to Kafka topic"),
            @ApiResponse(code = 404, message = "Failed to produce message to Kafka topic"),
            @ApiResponse(code = 500, message = "Failed to produce message to Kafka topic")
    })
    @PostMapping(value = "/producer")
    public ResponseEntity<String> send(@RequestBody String movieJSON) {
        String readTopicUri = "kafka:" + splitterConfig.getConsumerTopic() + "?" + buildURIParamsUsing(splitterConfig.getConsumerConfig());
        LOGGER.info("Sending [{}] data to [{}] endpoint", movieJSON, readTopicUri);
        producerTemplate.sendBody(readTopicUri, movieJSON);
        return ResponseEntity.ok().build();
    }

}
