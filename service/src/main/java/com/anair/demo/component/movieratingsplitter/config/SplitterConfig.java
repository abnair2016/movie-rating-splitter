package com.anair.demo.component.movieratingsplitter.config;

import lombok.Getter;
import org.apache.camel.component.kafka.KafkaConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

import static com.anair.demo.component.movieratingsplitter.util.SplitterConstants.*;

@Configuration
@Getter
public class SplitterConfig {

    private final String securityProtocol;
    private final String bootstrapServers;
    private final String consumerGroupId;
    private final String producerAcks;
    private final String producerRetries;
    private final String producerBatchSize;
    private final String producerLingerMs;
    private final String producerBuffer;
    private final String producerMaxRequestSize;
    private final Integer consumerSessionTimeout;
    private final Integer consumerRequestTimeout;
    private final Integer consumerMaxPartitionFetchBytes;
    private final Integer kafkaPollTimeout;
    private final String consumerTopic;
    private final String producerTopic;

    public SplitterConfig(@Value("${kafka.sec_protocol:PLAINTEXT}") final String securityProtocol,
                          @Value("${kafka.endpoint}") final String bootstrapServers,
                          @Value("${kafka.consumer.group.id}") final String consumerGroupId,
                          @Value("${kafka.producer.acks:all}") final String producerAcks,
                          @Value("${kafka.producer.retries:0}") final String producerRetries,
                          @Value("${kafka.producer.batch.size:16384}") final String producerBatchSize,
                          @Value("${kafka.producer.linger.ms:1}") final String producerLingerMs,
                          @Value("${kafka.producer.buffer:33554432}") final String producerBuffer,
                          @Value("${kafka.producer.max.request.size:1048576000}") final String producerMaxRequestSize,
                          @Value("${kafka.consumer.session.timeout.ms:120000}") final Integer consumerSessionTimeout,
                          @Value("${kafka.consumer.request.timeout.ms:180000}") final Integer consumerRequestTimeout,
                          @Value("${kafka.consumer.max.partition.fetch.bytes:1048576000}") final Integer consumerMaxPartitionFetchBytes,
                          @Value("${kafka.consumer.poll.timeout.ms:1000}") final Integer kafkaPollTimeout,
                          @Value("${kafka.consumer.topic}") final String consumerTopic,
                          @Value("${kafka.producer.topic}") final String producerTopic) {
        this.securityProtocol = securityProtocol;
        this.bootstrapServers = bootstrapServers;
        this.consumerGroupId = consumerGroupId;
        this.producerAcks = producerAcks;
        this.producerRetries = producerRetries;
        this.producerBatchSize = producerBatchSize;
        this.producerLingerMs = producerLingerMs;
        this.producerBuffer = producerBuffer;
        this.producerMaxRequestSize = producerMaxRequestSize;
        this.consumerSessionTimeout = consumerSessionTimeout;
        this.consumerRequestTimeout = consumerRequestTimeout;
        this.consumerMaxPartitionFetchBytes = consumerMaxPartitionFetchBytes;
        this.kafkaPollTimeout = kafkaPollTimeout;
        this.consumerTopic = consumerTopic;
        this.producerTopic = producerTopic;
    }

    public Properties getProducerConfig() {
        final Properties config = new Properties();
        config.put(SECURITY_PROTOCOL, this.securityProtocol);
        config.put(BROKERS, this.bootstrapServers);
        config.put(REQUEST_REQUIRED_ACKS, this.producerAcks);
        config.put(RETRIES, this.producerRetries);
        config.put(PRODUCER_BATCH_SIZE, this.producerBatchSize);
        config.put(LINGER_MS, this.producerLingerMs);
        config.put(BUFFER_MEMORY_SIZE, this.producerBuffer);
        config.put(MAX_REQUEST_SIZE, this.producerMaxRequestSize);
        config.put(KEY_SERIALIZER_CLASS, KafkaConstants.KAFKA_DEFAULT_SERIALIZER);
        config.put(SERIALIZER_CLASS, KafkaConstants.KAFKA_DEFAULT_SERIALIZER);

        return config;
    }

    public Properties getConsumerConfig() {
        final Properties config = new Properties();
        config.put(SECURITY_PROTOCOL, this.securityProtocol);
        config.put(BROKERS, this.bootstrapServers);
        config.put(GROUP_ID, this.consumerGroupId);
        config.put(SESSION_TIMEOUT_MS, this.consumerSessionTimeout);
        config.put(CONSUMER_REQUEST_TIMEOUT_MS, this.consumerRequestTimeout);
        config.put(MAX_PARTITION_FETCH_BYTES, this.consumerMaxPartitionFetchBytes);
        config.put(KEY_DESERIALIZER, KafkaConstants.KAFKA_DEFAULT_DESERIALIZER);
        config.put(VALUE_DESERIALIZER, KafkaConstants.KAFKA_DEFAULT_DESERIALIZER);

        return config;
    }

}
