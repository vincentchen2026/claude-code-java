package com.claudecode.services.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Stub for CA certificate management using java.security.KeyStore + TrustManagerFactory.
 * In production, this would download a CA cert and install it into the JVM trust store.
 */
public class CaCertManager {

    private static final Logger log = LoggerFactory.getLogger(CaCertManager.class);

    private KeyStore trustStore;
    private boolean initialized;

    public CaCertManager() {
        this.initialized = false;
    }

    /**
     * Initializes an empty trust store.
     */
    public void initTrustStore() {
        try {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            initialized = true;
            log.debug("Initialized empty trust store");
        } catch (Exception e) {
            log.warn("Failed to initialize trust store", e);
        }
    }

    /**
     * Adds a certificate (DER or PEM bytes) to the trust store.
     * Returns true on success.
     */
    public boolean addCertificate(String alias, byte[] certBytes) {
        if (!initialized) {
            initTrustStore();
        }
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream is = new ByteArrayInputStream(certBytes);
            Certificate cert = cf.generateCertificate(is);
            trustStore.setCertificateEntry(alias, cert);
            log.debug("Added certificate: {}", alias);
            return true;
        } catch (Exception e) {
            log.warn("Failed to add certificate: {}", alias, e);
            return false;
        }
    }

    /**
     * Creates a TrustManagerFactory from the current trust store.
     * Returns null if not initialized.
     */
    public TrustManagerFactory createTrustManagerFactory() {
        if (!initialized || trustStore == null) {
            return null;
        }
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            return tmf;
        } catch (Exception e) {
            log.warn("Failed to create TrustManagerFactory", e);
            return null;
        }
    }

    /**
     * Applies the trust store as the default SSLContext.
     * Stub: logs but does not actually override the JVM default in tests.
     */
    public boolean applyAsDefault() {
        TrustManagerFactory tmf = createTrustManagerFactory();
        if (tmf == null) return false;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            // In production: SSLContext.setDefault(sslContext);
            log.debug("SSL context prepared (not applied as default in stub mode)");
            return true;
        } catch (Exception e) {
            log.warn("Failed to apply SSL context", e);
            return false;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }
}
