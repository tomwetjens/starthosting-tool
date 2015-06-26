package nl.wetgos.starthosting.commands.dynamic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class PublicIPAddressWatcher {

    private final List<PublicIPAddressProvider> providers;
    private final long interval;

    public void watch(Consumer<String> publicIPAddressChanged) {
        String lastPublicIPAddress = null;

        log.info("Start watching public IP address");

        try {
            while (true) {
                String publicIPAddress = doCheck();

                if (publicIPAddress != null && !publicIPAddress.equals(lastPublicIPAddress)) {
                    log.info("Public IP address changed: {}", publicIPAddress);

                    publicIPAddressChanged.accept(publicIPAddress);

                    lastPublicIPAddress = publicIPAddress;
                }

                Thread.sleep(interval);
            }
        } catch (InterruptedException e) {
            log.debug("Thread interrupted");
        }

        log.info("Stopped watching public IP address");
    }

    private String doCheck() {
        for (PublicIPAddressProvider provider : providers) {
            try {
                return provider.getPublicIPAddress();
            } catch (Exception e) {
                log.warn("Could not get public IP address from provider " + provider, e);
            }
        }
        return null;
    }

}
