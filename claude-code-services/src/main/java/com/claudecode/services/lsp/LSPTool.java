package com.claudecode.services.lsp;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.claudecode.tools.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tool that queries file diagnostics from LSP servers.
 * Extends Tool&lt;JsonNode, String&gt;.
 */
public class LSPTool extends Tool<JsonNode, String> {

    private LspService lspService;

    public LSPTool() {
        this.lspService = null;
    }

    public LSPTool(LspService lspService) {
        this.lspService = lspService;
    }

    /**
     * Lazily initialize LspService on first use.
     */
    private LspService getOrCreateService() {
        if (lspService == null) {
            this.lspService = new LspService();
        }
        return lspService;
    }

    @Override
    public String name() {
        return "LSP";
    }

    @Override
    public String description() {
        return "Query file diagnostics (errors, warnings) from language servers";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode fileProp = mapper().createObjectNode();
        fileProp.put("type", "string");
        fileProp.put("description", "File path to get diagnostics for");
        props.set("filePath", fileProp);

        schema.set("required", mapper().createArrayNode().add("filePath"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String filePath = input.has("filePath") ? input.get("filePath").asText() : null;
        if (filePath == null || filePath.isBlank()) {
            return "Error: filePath is required";
        }

        LspService service = getOrCreateService();
        Path path = Path.of(filePath);
        List<Diagnostic> diagnostics = service.getDiagnostics(path);

        if (diagnostics.isEmpty()) {
            return "No diagnostics found for " + filePath;
        }

        return diagnostics.stream()
                .map(Diagnostic::format)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        return PermissionDecision.ALLOW;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
