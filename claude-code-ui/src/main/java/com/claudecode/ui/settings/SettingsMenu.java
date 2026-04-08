package com.claudecode.ui.settings;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Settings menu with hierarchical navigation.
 * Task 64.1: Settings panel
 */
public class SettingsMenu {

    private final PrintWriter writer;
    private final Consumer<String> onSelect;

    public SettingsMenu(PrintWriter writer) {
        this.writer = writer;
        this.onSelect = null;
    }

    public SettingsMenu(PrintWriter writer, Consumer<String> onSelect) {
        this.writer = writer;
        this.onSelect = onSelect;
    }

    public void renderMainMenu() {
        writer.println();
        writer.println(Ansi.styled("┌─ Settings ──────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [1] General");
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [2] Model");
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [3] API Keys");
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [4] Permissions");
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [5] Tools");
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [6] MCP Servers");
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [7] Appearance");
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [8] Advanced");
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [s]ave  [r]eset  [q]uit");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    public void renderSettingsGroup(String groupName, Map<String, SettingItem> settings) {
        writer.println();
        writer.println(Ansi.styled("┌─ Settings: " + groupName + " ─────────────────────────────", AnsiColor.CYAN));

        for (Map.Entry<String, SettingItem> entry : settings.entrySet()) {
            String key = entry.getKey();
            SettingItem item = entry.getValue();

            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + Ansi.colored(key, AnsiColor.WHITE) + ": " +
                formatSettingValue(item));
        }

        writer.println(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.flush();
    }

    private String formatSettingValue(SettingItem item) {
        return switch (item.type()) {
            case "boolean" -> item.value() != null && (Boolean) item.value()
                ? Ansi.colored("enabled", AnsiColor.GREEN)
                : Ansi.colored("disabled", AnsiColor.RED);
            case "string" -> item.value() != null
                ? Ansi.colored("\"" + item.value() + "\"", AnsiColor.YELLOW)
                : Ansi.styled("not set", AnsiStyle.DIM);
            case "number" -> item.value() != null
                ? Ansi.colored(String.valueOf(item.value()), AnsiColor.CYAN)
                : Ansi.styled("not set", AnsiStyle.DIM);
            default -> Ansi.styled(String.valueOf(item.value()), AnsiStyle.DIM);
        };
    }

    public void renderSkillsMenu(List<SkillInfo> skills) {
        writer.println();
        writer.println(Ansi.styled("┌─ Skills ─────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + skills.size() + " skill(s) available");
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));

        for (SkillInfo skill : skills) {
            String enabledStr = skill.enabled() ? Ansi.colored("●", AnsiColor.GREEN) : Ansi.colored("○", AnsiColor.GRAY);
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + enabledStr + " " +
                Ansi.colored(skill.name(), AnsiColor.WHITE));
            if (skill.description() != null && !skill.description().isEmpty()) {
                writer.println(Ansi.styled("│     ", AnsiColor.GRAY) +
                    Ansi.styled(truncate(skill.description(), 50), AnsiStyle.DIM));
            }
        }

        writer.println(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.flush();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public record SettingItem(String type, Object value, String description) {}
    public record SkillInfo(String name, String description, boolean enabled) {}
}