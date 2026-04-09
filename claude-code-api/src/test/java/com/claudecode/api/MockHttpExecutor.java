package com.claudecode.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;

/**
 * Mock HTTP executor for testing OpenAiCompatClient without making real network calls.
 */
public class MockHttpExecutor implements HttpExecutor {

    private String responseBody = "";
    private String errorBody = "";
    private int responseCode = 200;
    private IOException connectionException = null;
    private boolean throwOnSend = false;

    private final MockHttpURLConnection connection;

    public MockHttpExecutor() {
        this.connection = new MockHttpURLConnection();
    }

    public MockHttpExecutor withResponse(String body) {
        this.responseBody = body;
        return this;
    }

    public MockHttpExecutor withResponseCode(int code) {
        this.responseCode = code;
        return this;
    }

    public MockHttpExecutor withErrorBody(String body) {
        this.errorBody = body;
        return this;
    }

    public MockHttpExecutor withConnectionException(IOException e) {
        this.connectionException = e;
        return this;
    }

    public MockHttpExecutor thatThrowsOnSend() {
        this.throwOnSend = true;
        return this;
    }

    @Override
    public HttpURLConnection createConnection(String url) throws IOException {
        if (connectionException != null) {
            throw connectionException;
        }
        connection.setResponseCode(responseCode);
        connection.setResponseBody(responseBody);
        connection.setErrorBody(errorBody);
        return connection;
    }

    @Override
    public void sendRequest(HttpURLConnection conn, String body) throws IOException {
        if (throwOnSend) {
            throw new IOException("Send failed");
        }
    }

    @Override
    public String readResponseBody(HttpURLConnection conn) {
        return responseBody;
    }

    @Override
    public String readErrorBody(HttpURLConnection conn) {
        return errorBody;
    }

    @Override
    public int getResponseCode(HttpURLConnection conn) {
        return responseCode;
    }

    public static MockHttpExecutor success(String jsonResponse) {
        return new MockHttpExecutor().withResponse(jsonResponse).withResponseCode(200);
    }

    public static MockHttpExecutor error(int code, String errorMessage) {
        return new MockHttpExecutor().withErrorBody(errorMessage).withResponseCode(code);
    }

    /**
     * Mock HttpURLConnection that returns configurable responses.
     */
    private static class MockHttpURLConnection extends HttpURLConnection {
        private int responseCode = 200;
        private String responseBody = "";
        private String errorBody = "";
        private InputStream inputStream;
        private InputStream errorStream;

        MockHttpURLConnection() {
            super(null);
        }

        void setResponseCode(int code) {
            this.responseCode = code;
        }

        void setResponseBody(String body) {
            this.responseBody = body;
            this.inputStream = new ByteArrayInputStream(body.getBytes());
        }

        void setErrorBody(String body) {
            this.errorBody = body;
            if (body != null && !body.isEmpty()) {
                this.errorStream = new ByteArrayInputStream(body.getBytes());
            }
        }

        @Override
        public int getResponseCode() {
            return responseCode;
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public InputStream getErrorStream() {
            return errorStream;
        }

        @Override
        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }

        @Override
        public void disconnect() {
        }

        @Override
        public void connect() {
        }

        @Override
        public void setRequestMethod(String method) throws ProtocolException {
        }

        @Override
        public void setRequestProperty(String key, String value) {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }
    }
}