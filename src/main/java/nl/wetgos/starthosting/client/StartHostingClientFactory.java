package nl.wetgos.starthosting.client;

import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class StartHostingClientFactory {

    private final String baseUrl;
    private final String user;
    private final String password;


    public StartHostingClient createClient() throws IOException {
        StartHostingClient client = new StartHostingClient(baseUrl);

        client.login(user, password);

        return client;
    }
}
