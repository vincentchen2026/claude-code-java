package com.claudecode.tools.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("search")
public record SearchOutput(
    @JsonProperty("query") String query,
    @JsonProperty("results") java.util.List<SearchResult> results,
    @JsonProperty("totalResults") int totalResults,
    @JsonProperty("searchDurationMs") long searchDurationMs
) {
    public record SearchResult(
        @JsonProperty("title") String title,
        @JsonProperty("url") String url,
        @JsonProperty("snippet") String snippet,
        @JsonProperty("score") double score
    ) {}
}