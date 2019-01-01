package me.lucapette.kafka.streams;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.StreamsMetadata;

import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MetadataService {
    private final KafkaStreams streams;

    public MetadataService(final KafkaStreams streams) {
        this.streams = streams;
    }

    public List<HostStoreInfo> streamsMetadata() {
        final Collection<StreamsMetadata> metadata = streams.allMetadata();
        return mapInstancesToHostStoreInfo(metadata);
    }

    public List<HostStoreInfo> streamsMetadataForStore(final String store) {
        final Collection<StreamsMetadata> metadata = streams.allMetadataForStore(store);
        return mapInstancesToHostStoreInfo(metadata);
    }

    public <K> HostStoreInfo streamsMetadataForStoreAndKey(final String store,
                                                           final K key,
                                                           final Serializer<K> serializer) {
        final StreamsMetadata metadata = streams.metadataForKey(store, key, serializer);
        if (metadata == null) {
            throw new NotFoundException();
        }

        if (notReady(metadata)) {
            throw new RuntimeException(store);
        }

        return new HostStoreInfo(metadata.host(),
            metadata.port(),
            metadata.stateStoreNames());
    }

    private boolean notReady(final StreamsMetadata metadata) {
        return StreamsMetadata.NOT_AVAILABLE.host().equals(metadata.host()) && StreamsMetadata.NOT_AVAILABLE.port() == metadata.port();
    }

    private List<HostStoreInfo> mapInstancesToHostStoreInfo(
        final Collection<StreamsMetadata> metadata) {

        return metadata.stream().map(metadatum -> new HostStoreInfo(metadatum.host(),
                metadatum.port(),
                metadatum.stateStoreNames()))
            .collect(Collectors.toList());
    }
}

