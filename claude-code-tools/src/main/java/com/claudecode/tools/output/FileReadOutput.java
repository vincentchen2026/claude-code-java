package com.claudecode.tools.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

@JsonTypeName("file_read")
public record FileReadOutput(
    @JsonProperty("path") String path,
    @JsonProperty("content") String content,
    @JsonProperty("bytesRead") long bytesRead,
    @JsonProperty("lineCount") int lineCount
) {}