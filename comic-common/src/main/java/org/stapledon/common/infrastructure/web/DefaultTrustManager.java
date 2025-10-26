package org.stapledon.common.infrastructure.web;

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * WARNING: This TrustManager trusts ALL SSL certificates without validation.
 * This is intentionally insecure and should only be used in development/testing
 * environments. DO NOT use in production as it exposes the application to
 * man-in-the-middle attacks.
 */
public class DefaultTrustManager implements X509TrustManager {

    @Override
    @SuppressWarnings({"squid:S4424", "java:S4830"})
    public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
        // Intentionally empty - accepts all client certificates (INSECURE)
    }

    @Override
    @SuppressWarnings({"squid:S4424", "java:S4830"})
    public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
        // Intentionally empty - accepts all server certificates (INSECURE)
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
