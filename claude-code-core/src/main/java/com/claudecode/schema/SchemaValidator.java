package com.claudecode.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

public class SchemaValidator {

    private final Map<String, Schema> schemas;
    private final Map<String, BiFunction<JsonNode, Schema, ValidationResult>> semanticValidators;
    private final Map<String, Schema> registeredDiscriminators;
    private final JacksonSchemaGenerator jacksonSchemaGenerator;

    public SchemaValidator() {
        this.schemas = new ConcurrentHashMap<>();
        this.semanticValidators = new ConcurrentHashMap<>();
        this.registeredDiscriminators = new ConcurrentHashMap<>();
        this.jacksonSchemaGenerator = new JacksonSchemaGenerator();
    }

    public void registerSchema(String name, Schema schema) {
        schemas.put(name, schema);
    }

    public Schema getSchema(String name) {
        return schemas.get(name);
    }

    public void registerSemanticValidator(String typeName, BiFunction<JsonNode, Schema, ValidationResult> validator) {
        semanticValidators.put(typeName, validator);
    }

    public void registerDiscriminatedUnion(String discriminatorField, Schema schema) {
        registeredDiscriminators.put(discriminatorField, schema);
    }

    public ValidationResult validate(JsonNode value, Schema schema) {
        return validate(value, schema, true);
    }

    public ValidationResult validate(JsonNode value, Schema schema, boolean applySemanticValidation) {
        List<String> errors = new ArrayList<>();

        ValidationResult basicResult = validateBasic(value, schema);
        if (!basicResult.isSuccess()) {
            errors.addAll(basicResult.errors());
        }

        if (basicResult.isSuccess() && applySemanticValidation) {
            ValidationResult semanticResult = validateSemantic(value, schema);
            if (!semanticResult.isSuccess()) {
                errors.addAll(semanticResult.errors());
            }
        }

        return errors.isEmpty() 
            ? ValidationResult.ok() 
            : new ValidationResult(false, errors);
    }

    private ValidationResult validateBasic(JsonNode value, Schema schema) {
        return switch (schema.kind()) {
            case NULL -> validateNull(value);
            case BOOLEAN -> validateBoolean(value);
            case STRING -> validateString(value, schema.stringConstraint());
            case NUMBER -> validateNumber(value, schema.numberConstraint());
            case INTEGER -> validateInteger(value, schema.numberConstraint());
            case ARRAY -> validateArray(value, schema.arrayConstraint());
            case OBJECT -> validateObject(value, schema.objectConstraint(), schema.properties(), schema.requiredProperties());
            case UNION -> validateUnion(value, schema.variants(), schema.discriminator());
            case ENUM -> validateEnum(value, schema.enumValues());
            case DISCRIMINATED_UNION -> validateDiscriminatedUnion(value, schema);
        };
    }

    private ValidationResult validateSemantic(JsonNode value, Schema schema) {
        if (schema.typeName() != null && semanticValidators.containsKey(schema.typeName())) {
            return semanticValidators.get(schema.typeName()).apply(value, schema);
        }
        return ValidationResult.ok();
    }

    private ValidationResult validateNull(JsonNode value) {
        if (value.isNull()) {
            return ValidationResult.ok();
        }
        return ValidationResult.failure("Expected null");
    }

    private ValidationResult validateBoolean(JsonNode value) {
        if (value.isBoolean()) {
            return ValidationResult.ok();
        }
        return ValidationResult.failure("Expected boolean");
    }

    private ValidationResult validateString(JsonNode value, StringConstraint constraint) {
        if (!value.isTextual()) {
            return ValidationResult.failure("Expected string");
        }
        if (constraint == null) {
            return ValidationResult.ok();
        }
        String str = value.asText();
        if (constraint.minLength() != null && str.length() < constraint.minLength()) {
            return ValidationResult.failure("String too short: minimum " + constraint.minLength());
        }
        if (constraint.maxLength() != null && str.length() > constraint.maxLength()) {
            return ValidationResult.failure("String too long: maximum " + constraint.maxLength());
        }
        if (constraint.pattern() != null && !constraint.pattern().matcher(str).matches()) {
            return ValidationResult.failure("String does not match pattern: " + constraint.pattern().pattern());
        }
        if (constraint.format() != null && !validateFormat(str, constraint.format())) {
            return ValidationResult.failure("String does not match format: " + constraint.format());
        }
        return ValidationResult.ok();
    }

    private boolean validateFormat(String value, String format) {
        return switch (format) {
            case "uri" -> validateUri(value);
            case "email" -> validateEmail(value);
            case "uuid" -> validateUuid(value);
            case "date-time" -> validateDateTime(value);
            case "date" -> validateDate(value);
            case "time" -> validateTime(value);
            case "ipv4" -> validateIpv4(value);
            case "ipv6" -> validateIpv6(value);
            default -> true;
        };
    }

    private boolean validateUri(String value) {
        try {
            java.net.URI.create(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateEmail(String value) {
        return Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matcher(value).matches();
    }

    private boolean validateUuid(String value) {
        return Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").matcher(value).matches();
    }

    private boolean validateDateTime(String value) {
        try {
            java.time.Instant.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateDate(String value) {
        try {
            java.time.LocalDate.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateTime(String value) {
        try {
            java.time.LocalTime.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateIpv4(String value) {
        String[] parts = value.split("\\.");
        if (parts.length != 4) return false;
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private boolean validateIpv6(String value) {
        try {
            java.net.InetAddress.getByName(value);
            return value.contains(":");
        } catch (Exception e) {
            return false;
        }
    }

    private ValidationResult validateNumber(JsonNode value, NumberConstraint constraint) {
        if (!value.isNumber()) {
            return ValidationResult.failure("Expected number");
        }
        if (constraint == null) {
            return ValidationResult.ok();
        }
        double num = value.asDouble();
        if (constraint.minimum() != null && num < constraint.minimum()) {
            return ValidationResult.failure("Number below minimum: " + constraint.minimum());
        }
        if (constraint.maximum() != null && num > constraint.maximum()) {
            return ValidationResult.failure("Number above maximum: " + constraint.maximum());
        }
        if (constraint.exclusiveMin() != null && num <= constraint.exclusiveMin()) {
            return ValidationResult.failure("Number must be greater than: " + constraint.exclusiveMin());
        }
        if (constraint.exclusiveMax() != null && num >= constraint.exclusiveMax()) {
            return ValidationResult.failure("Number must be less than: " + constraint.exclusiveMax());
        }
        if (constraint.multipleOf() != null && num % constraint.multipleOf() != 0) {
            return ValidationResult.failure("Number must be multiple of: " + constraint.multipleOf());
        }
        return ValidationResult.ok();
    }

    private ValidationResult validateInteger(JsonNode value, NumberConstraint constraint) {
        if (!value.isIntegralNumber()) {
            return ValidationResult.failure("Expected integer");
        }
        return validateNumber(value, constraint);
    }

    private ValidationResult validateArray(JsonNode value, ArrayConstraint constraint) {
        if (!value.isArray()) {
            return ValidationResult.failure("Expected array");
        }
        if (constraint == null) {
            return ValidationResult.ok();
        }
        int size = value.size();
        if (constraint.minItems() != null && size < constraint.minItems()) {
            return ValidationResult.failure("Array too short: minimum " + constraint.minItems());
        }
        if (constraint.maxItems() != null && size > constraint.maxItems()) {
            return ValidationResult.failure("Array too long: maximum " + constraint.maxItems());
        }
        if (constraint.uniqueItems() && hasDuplicates(value)) {
            return ValidationResult.failure("Array must have unique items");
        }
        if (constraint.items() != null) {
            for (int i = 0; i < size; i++) {
                ValidationResult itemResult = validate(value.get(i), constraint.items());
                if (!itemResult.isSuccess()) {
                    return new ValidationResult(false, List.of("Item at index " + i + " failed: " + itemResult.errors().get(0)));
                }
            }
        }
        return ValidationResult.ok();
    }

    private boolean hasDuplicates(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            String str = item.toString();
            if (values.contains(str)) {
                return true;
            }
            values.add(str);
        }
        return false;
    }

    private ValidationResult validateObject(JsonNode value, ObjectConstraint constraint, Map<String, Schema> properties, List<String> required) {
        if (!value.isObject()) {
            return ValidationResult.failure("Expected object");
        }
        ObjectNode obj = (ObjectNode) value;

        if (constraint == null && properties == null) {
            return ValidationResult.ok();
        }

        if (constraint != null) {
            int size = value.size();
            if (constraint.minProperties() != null && size < constraint.minProperties()) {
                return ValidationResult.failure("Object has too few properties: minimum " + constraint.minProperties());
            }
            if (constraint.maxProperties() != null && size > constraint.maxProperties()) {
                return ValidationResult.failure("Object has too many properties: maximum " + constraint.maxProperties());
            }
            if (constraint.additionalProperties() != null && !constraint.additionalProperties().allowed()) {
                if (properties != null) {
                    var iter = obj.fieldNames();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        if (!properties.containsKey(key)) {
                            return ValidationResult.failure("Additional property not allowed: " + key);
                        }
                    }
                }
            }
        }

        if (required != null) {
            for (String field : required) {
                if (!obj.has(field) || obj.get(field).isNull()) {
                    return ValidationResult.failure("Required property missing: " + field);
                }
            }
        }

        if (properties != null) {
            var iter = obj.fields();
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                Schema propSchema = properties.get(entry.getKey());
                if (propSchema != null) {
                    ValidationResult propResult = validate(entry.getValue(), propSchema);
                    if (!propResult.isSuccess()) {
                        return new ValidationResult(false, List.of("Property '" + entry.getKey() + "': " + propResult.errors().get(0)));
                    }
                }
            }
        }

        return ValidationResult.ok();
    }

    private ValidationResult validateUnion(JsonNode value, List<Schema> variants, String discriminator) {
        if (variants == null || variants.isEmpty()) {
            return ValidationResult.failure("Union has no variants");
        }

        List<String> allErrors = new ArrayList<>();
        for (Schema variant : variants) {
            ValidationResult result = validate(value, variant);
            if (result.isSuccess()) {
                return result;
            }
            allErrors.addAll(result.errors());
        }
        return new ValidationResult(false, List.of("Value does not match any union variant: " + String.join("; ", allErrors)));
    }

    private ValidationResult validateDiscriminatedUnion(JsonNode value, Schema schema) {
        if (!value.isObject()) {
            return ValidationResult.failure("Discriminated union requires object");
        }
        if (schema.discriminator() == null || schema.variants() == null) {
            return ValidationResult.failure("Discriminated union missing discriminator or variants");
        }

        ObjectNode obj = (ObjectNode) value;
        JsonNode discriminatorValue = obj.get(schema.discriminator());
        if (discriminatorValue == null || !discriminatorValue.isTextual()) {
            return ValidationResult.failure("Missing discriminator field: " + schema.discriminator());
        }

        String variantKey = discriminatorValue.asText();
        Schema matchingVariant = schema.variants().stream()
            .filter(v -> variantKey.equals(v.variantName()))
            .findFirst()
            .orElse(null);

        if (matchingVariant == null) {
            return ValidationResult.failure("Unknown variant: " + variantKey);
        }

        return validate(obj, matchingVariant);
    }

    private ValidationResult validateEnum(JsonNode value, List<String> enumValues) {
        if (enumValues == null || !value.isTextual()) {
            return ValidationResult.failure("Expected enum value");
        }
        if (enumValues.contains(value.asText())) {
            return ValidationResult.ok();
        }
        return ValidationResult.failure("Value not in enum: " + value.asText() + " (allowed: " + enumValues + ")");
    }

    public JsonNode generateJsonSchema(Schema schema) {
        return jacksonSchemaGenerator.generate(schema);
    }

    public String generateJsonSchemaString(Schema schema) {
        return jacksonSchemaGenerator.generate(schema).toString();
    }

    private static class JacksonSchemaGenerator {
        private final Map<Schema, JsonNode> cache = new ConcurrentHashMap<>();

        JsonNode generate(Schema schema) {
            return switch (schema.kind()) {
                case NULL -> generateNullSchema();
                case BOOLEAN -> generateBooleanSchema();
                case STRING -> generateStringSchema(schema.stringConstraint());
                case NUMBER -> generateNumberSchema(schema.numberConstraint());
                case INTEGER -> generateIntegerSchema(schema.numberConstraint());
                case ARRAY -> generateArraySchema(schema.arrayConstraint());
                case OBJECT -> generateObjectSchema(schema.properties(), schema.requiredProperties(), schema.objectConstraint());
                case UNION -> generateUnionSchema(schema.variants());
                case ENUM -> generateEnumSchema(schema.enumValues());
                case DISCRIMINATED_UNION -> generateDiscriminatedUnionSchema(schema);
            };
        }

        private JsonNode generateNullSchema() {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "null");
            return toJsonNode(schema);
        }

        private JsonNode generateBooleanSchema() {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "boolean");
            return toJsonNode(schema);
        }

        private JsonNode generateStringSchema(StringConstraint constraint) {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "string");
            if (constraint != null) {
                if (constraint.minLength() != null) schema.put("minLength", constraint.minLength());
                if (constraint.maxLength() != null) schema.put("maxLength", constraint.maxLength());
                if (constraint.pattern() != null) schema.put("pattern", constraint.pattern().pattern());
                if (constraint.format() != null) schema.put("format", constraint.format());
            }
            return toJsonNode(schema);
        }

        private JsonNode generateNumberSchema(NumberConstraint constraint) {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "number");
            addNumberConstraints(schema, constraint);
            return toJsonNode(schema);
        }

        private JsonNode generateIntegerSchema(NumberConstraint constraint) {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "integer");
            addNumberConstraints(schema, constraint);
            return toJsonNode(schema);
        }

        private void addNumberConstraints(Map<String, Object> schema, NumberConstraint constraint) {
            if (constraint != null) {
                if (constraint.minimum() != null) schema.put("minimum", constraint.minimum());
                if (constraint.maximum() != null) schema.put("maximum", constraint.maximum());
                if (constraint.exclusiveMin() != null) schema.put("exclusiveMinimum", constraint.exclusiveMin());
                if (constraint.exclusiveMax() != null) schema.put("exclusiveMaximum", constraint.exclusiveMax());
                if (constraint.multipleOf() != null) schema.put("multipleOf", constraint.multipleOf());
            }
        }

        private JsonNode generateArraySchema(ArrayConstraint constraint) {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "array");
            if (constraint != null) {
                if (constraint.minItems() != null) schema.put("minItems", constraint.minItems());
                if (constraint.maxItems() != null) schema.put("maxItems", constraint.maxItems());
                if (constraint.uniqueItems()) schema.put("uniqueItems", true);
                if (constraint.items() != null) schema.put("items", generate(constraint.items()));
            }
            return toJsonNode(schema);
        }

        private JsonNode generateObjectSchema(Map<String, Schema> properties, List<String> required, ObjectConstraint constraint) {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "object");
            if (properties != null) {
                Map<String, JsonNode> props = new HashMap<>();
                properties.forEach((k, v) -> props.put(k, generate(v)));
                schema.put("properties", props);
            }
            if (required != null && !required.isEmpty()) {
                schema.put("required", required);
            }
            if (constraint != null) {
                if (constraint.minProperties() != null) schema.put("minProperties", constraint.minProperties());
                if (constraint.maxProperties() != null) schema.put("maxProperties", constraint.maxProperties());
            }
            return toJsonNode(schema);
        }

        private JsonNode generateUnionSchema(List<Schema> variants) {
            Map<String, Object> schema = new HashMap<>();
            schema.put("oneOf", variants.stream().map(this::generate).toList());
            return toJsonNode(schema);
        }

        private JsonNode generateEnumSchema(List<String> enumValues) {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "string");
            schema.put("enum", enumValues);
            return toJsonNode(schema);
        }

        private JsonNode generateDiscriminatedUnionSchema(Schema schema) {
            Map<String, Object> result = new HashMap<>();
            result.put("oneOf", schema.variants().stream().map(v -> {
                Map<String, Object> variant = new HashMap<>();
                variant.put("if", Map.of("properties", Map.of(schema.discriminator(), Map.of("const", v.variantName()))));
                variant.put("then", generate(v));
                return variant;
            }).toList());
            return toJsonNode(result);
        }

        private JsonNode toJsonNode(Map<String, Object> map) {
            ObjectNode node = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            map.forEach((key, value) -> {
                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nestedMap = (Map<String, Object>) value;
                    node.set(key, toJsonNode(nestedMap));
                } else if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) value;
                    ArrayNode arrayNode = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
                    list.forEach(item -> {
                        if (item instanceof JsonNode) {
                            arrayNode.add((JsonNode) item);
                        } else if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> nestedMap = (Map<String, Object>) item;
                            arrayNode.add(toJsonNode(nestedMap));
                        } else {
                            arrayNode.add(item.toString());
                        }
                    });
                    node.set(key, arrayNode);
                } else if (value instanceof Integer) {
                    node.put(key, (Integer) value);
                } else if (value instanceof Long) {
                    node.put(key, (Long) value);
                } else if (value instanceof Double) {
                    node.put(key, (Double) value);
                } else if (value instanceof Boolean) {
                    node.put(key, (Boolean) value);
                } else if (value != null) {
                    node.put(key, value.toString());
                }
            });
            return node;
        }
    }

    public enum SchemaKind { NULL, BOOLEAN, STRING, NUMBER, INTEGER, ARRAY, OBJECT, UNION, ENUM, DISCRIMINATED_UNION }

    public record StringConstraint(Integer minLength, Integer maxLength, Pattern pattern, String format) {}

    public record NumberConstraint(Double minimum, Double maximum, Double exclusiveMin, Double exclusiveMax, Double multipleOf) {}

    public record ArrayConstraint(Integer minItems, Integer maxItems, boolean uniqueItems, Schema items) {}

    public record ObjectConstraint(Integer minProperties, Integer maxProperties, AdditionalProperties additionalProperties) {}

    public record AdditionalProperties(boolean allowed) {}

    public record Schema(
        SchemaKind kind,
        String typeName,
        List<Schema> variants,
        Map<String, Schema> properties,
        List<String> requiredProperties,
        StringConstraint stringConstraint,
        NumberConstraint numberConstraint,
        ArrayConstraint arrayConstraint,
        ObjectConstraint objectConstraint,
        List<String> enumValues,
        String discriminator,
        String variantName
    ) {
        public static Schema null_() { return new Schema(SchemaKind.NULL, null, null, null, null, null, null, null, null, null, null, null); }
        public static Schema bool() { return new Schema(SchemaKind.BOOLEAN, null, null, null, null, null, null, null, null, null, null, null); }
        public static Schema string() { return new Schema(SchemaKind.STRING, null, null, null, null, new StringConstraint(null, null, null, null), null, null, null, null, null, null); }
        public static Schema string(StringConstraint constraint) { return new Schema(SchemaKind.STRING, null, null, null, null, constraint, null, null, null, null, null, null); }
        public static Schema number() { return new Schema(SchemaKind.NUMBER, null, null, null, null, null, new NumberConstraint(null, null, null, null, null), null, null, null, null, null); }
        public static Schema number(NumberConstraint constraint) { return new Schema(SchemaKind.NUMBER, null, null, null, null, null, constraint, null, null, null, null, null); }
        public static Schema integer() { return new Schema(SchemaKind.INTEGER, null, null, null, null, null, new NumberConstraint(null, null, null, null, null), null, null, null, null, null); }
        public static Schema integer(NumberConstraint constraint) { return new Schema(SchemaKind.INTEGER, null, null, null, null, null, constraint, null, null, null, null, null); }
        public static Schema array(Schema items) { return new Schema(SchemaKind.ARRAY, null, null, null, null, null, null, new ArrayConstraint(null, null, false, items), null, null, null, null); }
        public static Schema array(Integer minItems, Integer maxItems, boolean uniqueItems, Schema items) { return new Schema(SchemaKind.ARRAY, null, null, null, null, null, null, new ArrayConstraint(minItems, maxItems, uniqueItems, items), null, null, null, null); }
        public static Schema object(Map<String, Schema> props, List<String> required) { return new Schema(SchemaKind.OBJECT, null, null, props, required, null, null, null, new ObjectConstraint(null, null, null), null, null, null); }
        public static Schema object(Map<String, Schema> props, List<String> required, ObjectConstraint constraint) { return new Schema(SchemaKind.OBJECT, null, null, props, required, null, null, null, constraint, null, null, null); }
        public static Schema union(List<Schema> variants) { return new Schema(SchemaKind.UNION, null, variants, null, null, null, null, null, null, null, null, null); }
        public static Schema enum_(List<String> values) { return new Schema(SchemaKind.ENUM, null, null, null, null, null, null, null, null, values, null, null); }
        public static Schema discriminatedUnion(String discriminator, List<Schema> variants) { return new Schema(SchemaKind.DISCRIMINATED_UNION, null, variants, null, null, null, null, null, null, null, discriminator, null); }
        public Schema withTypeName(String typeName) { return new Schema(kind, typeName, variants, properties, requiredProperties, stringConstraint, numberConstraint, arrayConstraint, objectConstraint, enumValues, discriminator, variantName); }
    }

    public record ValidationResult(boolean successful, List<String> errors) {
        public static ValidationResult ok() { return new ValidationResult(true, List.of()); }
        public static ValidationResult failure(String error) { return new ValidationResult(false, List.of(error)); }
        public static ValidationResult failure(List<String> errors) { return new ValidationResult(false, errors); }
        public boolean isSuccess() { return successful; }
        public boolean isFailure() { return !successful; }
    }
}
