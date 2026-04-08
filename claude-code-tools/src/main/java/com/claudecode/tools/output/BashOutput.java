package com.claudecode.tools.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("bash")
public record BashOutput(
    @JsonProperty("stdout") String stdout,
    @JsonProperty("stderr") String stderr,
    @JsonProperty("exitCode") int exitCode,
    @JsonProperty("durationMs") long durationMs
) {
    public boolean isSuccess() {
        return exitCode == 0;
    }
}