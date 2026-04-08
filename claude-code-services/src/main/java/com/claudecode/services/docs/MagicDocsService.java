package com.claudecode.services.docs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MagicDocsService {

    private static final Logger log = LoggerFactory.getLogger(MagicDocsService.class);

    private final Map<String, DocTemplate> templates = new ConcurrentHashMap<>();
    private final Path outputDir;

    public MagicDocsService() {
        this(Path.of(System.getProperty("user.home"), ".claude", "docs"));
    }

    public MagicDocsService(Path outputDir) {
        this.outputDir = outputDir;
    }

    public String generateDoc(DocRequest request) {
        DocTemplate template = templates.get(request.templateId());
        if (template == null) {
            template = getDefaultTemplate(request.templateId());
        }

        String content = applyTemplate(template, request.variables());

        DocResult result = new DocResult(
            "doc_" + System.currentTimeMillis(),
            request.templateId(),
            content,
            Instant.now(),
            true,
            null
        );

        log.info("Generated document from template: {}", request.templateId());
        return content;
    }

    public void registerTemplate(String templateId, DocTemplate template) {
        templates.put(templateId, template);
        log.info("Registered document template: {}", templateId);
    }

    private DocTemplate getDefaultTemplate(String templateId) {
        return new DocTemplate(
            templateId,
            "Default Template",
            "# Document\n\n{{content}}",
            Map.of("content", "Content goes here")
        );
    }

    private String applyTemplate(DocTemplate template, Map<String, Object> variables) {
        String content = template.content();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            content = content.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return content;
    }

    public DocTemplate getTemplate(String templateId) {
        return templates.get(templateId);
    }

    public record DocRequest(
        String templateId,
        Map<String, Object> variables
    ) {}

    public record DocTemplate(
        String templateId,
        String name,
        String content,
        Map<String, String> placeholders
    ) {}

    public record DocResult(
        String docId,
        String templateId,
        String content,
        Instant generatedAt,
        boolean success,
        String errorMessage
    ) {}
}