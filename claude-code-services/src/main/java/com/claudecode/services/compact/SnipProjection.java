package com.claudecode.services.compact;

import com.claudecode.core.message.Message;

import java.util.List;

public class SnipProjection {

    private final int maxSnipLength;
    private final int overlapChars;

    public SnipProjection() {
        this(500, 50);
    }

    public SnipProjection(int maxSnipLength, int overlapChars) {
        this.maxSnipLength = maxSnipLength;
        this.overlapChars = overlapChars;
    }

    public List<SnipSegment> project(Message message) {
        String content = extractContent(message);
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        if (content.length() <= maxSnipLength) {
            return List.of(new SnipSegment(0, content, true));
        }

        return createOverlappingSegments(content);
    }

    public List<SnipSegment> projectMulti(List<Message> messages) {
        return messages.stream()
            .flatMap(msg -> project(msg).stream())
            .toList();
    }

    private List<SnipSegment> createOverlappingSegments(String content) {
        var segments = new java.util.ArrayList<SnipSegment>();
        int pos = 0;
        int segmentIndex = 0;

        while (pos < content.length()) {
            int end = Math.min(pos + maxSnipLength, content.length());
            boolean isLast = end >= content.length();
            
            if (!isLast && end < content.length()) {
                end = findGoodSplitPoint(content, end);
            }

            String segmentText = content.substring(pos, end);
            segments.add(new SnipSegment(segmentIndex++, segmentText, isLast));

            pos = end - overlapChars;
            if (pos <= segments.get(segments.size() - 1).endIndex()) {
                pos = segments.get(segments.size() - 1).endIndex() + 1;
            }
        }

        return segments;
    }

    private int findGoodSplitPoint(String content, int end) {
        char[] chars = content.toCharArray();
        for (int i = end - 1; i >= Math.max(0, end - 100); i--) {
            if (Character.isWhitespace(chars[i]) || Character.isLetterOrDigit(chars[i])) {
                continue;
            }
            if (chars[i] == '.' || chars[i] == ',' || chars[i] == ';' || chars[i] == ':') {
                return i + 1;
            }
        }
        return end;
    }

    private String extractContent(Message message) {
        return switch (message) {
            case com.claudecode.core.message.UserMessage um -> um.message().toString();
            case com.claudecode.core.message.AssistantMessage am -> am.message().toString();
            case com.claudecode.core.message.SystemMessage sm -> sm.content();
            default -> message.type();
        };
    }

    public record SnipSegment(
        int index,
        String text,
        boolean isLast
    ) {
        public int endIndex() {
            return text.length();
        }
    }
}