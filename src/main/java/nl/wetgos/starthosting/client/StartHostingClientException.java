package nl.wetgos.starthosting.client;

public class StartHostingClientException extends RuntimeException {

    public StartHostingClientException(String message) {
        super(message);
    }

    public StartHostingClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
