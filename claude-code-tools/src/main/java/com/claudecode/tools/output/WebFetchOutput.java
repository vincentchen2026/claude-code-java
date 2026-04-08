package com.claudecode.tools.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("web_fetch")
public record WebFetchOutput(
    @JsonProperty("url") String url,
    @JsonProperty("content") String content,
    @JsonProperty("statusCode") int statusCode,
    @JsonProperty("headers") java.util.Map<String, String> headers,
    @JsonProperty("fetchDurationMs") long fetchDurationMs
) {
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
}