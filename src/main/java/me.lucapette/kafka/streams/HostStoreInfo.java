package me.lucapette.kafka.streams;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@AllArgsConstructor
@Data
public class HostStoreInfo {
    private String host;
    private int port;
    private Set<String> storeNames;
}
