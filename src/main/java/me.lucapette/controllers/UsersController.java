package me.lucapette.controllers;

import lombok.extern.java.Log;
import me.lucapette.kafka.streams.HostStoreInfo;
import me.lucapette.kafka.streams.MetadataService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


@Log
@RestController
public class UsersController {
    static final String USERS_STORE = "usersStore";
    @Value("${kafka.brokers}")
    private String kafkaBrokers;
    @Value("${rpc.host}")
    private String rpcHost;
    @Value("${rpc.port}")
    private int rpcPort;

    private KafkaStreams streams;

    private RestTemplate restTemplate = new RestTemplate();

    private MetadataService metadataService;

    private ReadOnlyKeyValueStore<String, String> usersStore() {
        return streams.store(USERS_STORE, QueryableStoreTypes.keyValueStore());
    }

    @GetMapping("/health")
    ResponseEntity health() {
        usersStore();

        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{id}")
    public String getUserById(@PathVariable("id") String id, HttpServletRequest request) {
        HostStoreInfo streamsMetadata = metadataService.streamsMetadataForStoreAndKey(USERS_STORE, id, new StringSerializer());

        log.info(streamsMetadata.toString());

        if (!thisHost(streamsMetadata)) {
            String url = "http://" + streamsMetadata.getHost() + ":" + streamsMetadata.getPort() + request.getRequestURI();

            log.info("getting from remote host: " + url);

            return restTemplate.getForEntity(url, String.class).getBody();
        }

        return usersStore().get(id);
    }

    @GetMapping("/users")
    public String getUsers() {
        ReadOnlyKeyValueStore<String, String> store = usersStore();

        log.info("before");
        for (int i = 0; i < 100_000; i++) {
            store.get("" + i);
        }
        log.info("after");

        return "ok";
    }

    @PostConstruct
    public void start() {
        log.info("start");

        Properties props = new Properties();

        String applicationId = "users";
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.CLIENT_ID_CONFIG, applicationId); // accept client id env
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, rpcHost + ":" + rpcPort);

        StreamsBuilder builder = new StreamsBuilder();

        builder.table("users", Materialized.as(USERS_STORE));

        streams = new KafkaStreams(builder.build(), props);
        metadataService = new MetadataService(streams);
        streams.start();
    }

    private boolean thisHost(final HostStoreInfo host) {
        return host.getHost().equals(rpcHost) && host.getPort() == rpcPort;
    }
}
