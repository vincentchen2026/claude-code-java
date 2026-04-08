package com.claudecode.services.auth;

import java.util.Optional;

/**
 * System keychain integration interface.
 * Supports macOS Keychain and Linux Secret Service.
 */
public interface SystemKeychain {

    /**
     * Stores a secret in the system keychain.
     */
    void store(String service, String account, String secret);

    /**
     * Retrieves a secret from the system keychain.
     */
    Optional<String> retrieve(String service, String account);

    /**
     * Deletes a secret from the system keychain.
     */
    boolean delete(String service, String account);

    /**
     * Returns whether the system keychain is available.
     */
    boolean isAvailable();

    /**
     * In-memory fallback implementation (no actual keychain integration).
     */
    static SystemKeychain inMemory() {
        return new InMemoryKeychain();
    }
}

class InMemoryKeychain implements SystemKeychain {

    private final java.util.Map<String, String> store = new java.util.concurrent.ConcurrentHashMap<>();

    private String key(String service, String account) {
        return service + ":" + account;
    }

    @Override
    public void store(String service, String account, String secret) {
        store.put(key(service, account), secret);
    }

    @Override
    public Optional<String> retrieve(String service, String account) {
        return Optional.ofNullable(store.get(key(service, account)));
    }

    @Override
    public boolean delete(String service, String account) {
        return store.remove(key(service, account)) != null;
    }

    @Override
    public boolean isAvailable() {
        return true; // In-memory is always available
    }
}
