package com.crawler.transdep.eticket.util;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;

public class HttpClientBuilder {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientBuilder.class);

    public static CloseableHttpClient createHttpClientWithInsecureSsl() throws Exception {
        logger.warn("Creating HTTP client with disabled SSL verification - for development/testing only!");

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        SSLContext sslContext = SSLContexts.custom()
            .loadTrustMaterial(keyStore, (chain, authType) -> true)
            .build();

        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
            sslContext,
            NoopHostnameVerifier.INSTANCE
        );

        return HttpClients.custom()
            .setSSLSocketFactory(sslSocketFactory)
            .build();
    }

    public static CloseableHttpClient createDefaultHttpClient() throws Exception {
        try {
            return createHttpClientWithInsecureSsl();
        } catch (Exception e) {
            logger.error("Failed to create custom HTTP client, using default", e);
            return HttpClients.createDefault();
        }
    }
}
