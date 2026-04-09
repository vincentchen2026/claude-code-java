package com.claudecode.api;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Pluggable HTTP execution layer for OpenAiCompatClient.
 * Allows mocking HTTP calls in tests without making real network requests.
 */
public interface HttpExecutor {

    /**
     * Creates a new HTTP connection to the given URL.
     */
    HttpURLConnection createConnection(String url) throws IOException;

    /**
     * Sends the request body to the connection.
     */
    void sendRequest(HttpURLConnection conn, String body) throws IOException;

    /**
     * Reads the response body from the connection.
     */
    String readResponseBody(HttpURLConnection conn) throws IOException;

    /**
     * Reads the error body from the connection.
     */
    String readErrorBody(HttpURLConnection conn) throws IOException;

    /**
     * Returns the response code from the connection.
     */
    int getResponseCode(HttpURLConnection conn) throws IOException;
}