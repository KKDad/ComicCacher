package org.stapledon.infrastructure.web;

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class DefaultTrustManager implements X509TrustManager {

    @Override
    @SuppressWarnings({"squid:S4424", "java:S4830"})
    public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
        // All certs are trusted
    }

    @Override
    @SuppressWarnings({"squid:S4424", "java:S4830"})
    public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
        // All servers are trusted
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
