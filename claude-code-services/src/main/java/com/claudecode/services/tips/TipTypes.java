package com.claudecode.services.tips;

public enum TipTypes {

    GENERAL("general", "General tips and hints", 1),
    KEYBOARD_SHORTCUT("keyboard", "Keyboard shortcuts", 2),
    TOOL_USAGE("tool", "Tips about using tools", 3),
    AGENT("agent", "Agent-related tips", 3),
    WORKFLOW("workflow", "Workflow optimization tips", 2),
    DEBUG("debug", "Debugging and troubleshooting", 2),
    PERFORMANCE("performance", "Performance tips", 2),
    SAFETY("safety", "Safety and security tips", 4);

    private final String id;
    private final String description;
    private final int priority;

    TipTypes(String id, String description, int priority) {
        this.id = id;
        this.description = description;
        this.priority = priority;
    }

    public String id() {
        return id;
    }

    public String description() {
        return description;
    }

    public int priority() {
        return priority;
    }

    public static TipTypes fromId(String id) {
        for (TipTypes type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return GENERAL;
    }
}