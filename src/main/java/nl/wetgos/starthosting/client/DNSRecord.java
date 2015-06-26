package nl.wetgos.starthosting.client;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DNSRecord {

    private String id;

    private String type;
    private String name;
    private String content;

}
