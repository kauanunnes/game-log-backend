package com.kauan.gamelog.shared;

import static java.util.Comparator.comparing;

import jakarta.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * JSON Merge Patch (RFC 7396): campo ausente fica como está, {@code null} limpa e objeto aninhado é mesclado.
 * O resultado passa pela mesma validação de um PUT.
 */
@Component
public class JsonMergePatch {
    private final JsonMapper mapper;
    private final Validator validator;

    public JsonMergePatch(JsonMapper mapper, Validator validator) {
        this.mapper = mapper;
        this.validator = validator;
    }

    @SuppressWarnings("unchecked")
    public <T> T apply(T current, JsonNode patch) {
        T result;
        try {
            result = (T) mapper.treeToValue(merge(mapper.valueToTree(current), patch), current.getClass());
        } catch (JacksonException e) {
            String field = e.getPath().stream()
                    .map(JacksonException.Reference::getPropertyName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));
            throw UnprocessableException.field("INVALID_VALUE", field, "valor inválido");
        }
        List<FieldIssue> errors = validator.validate(result).stream()
                .map(violation -> new FieldIssue(violation.getPropertyPath().toString(), violation.getMessage()))
                .sorted(comparing(FieldIssue::field))
                .toList();
        if (!errors.isEmpty()) {
            throw new UnprocessableException("INVALID_FIELDS", "Confira os campos indicados.", errors);
        }
        return result;
    }

    private JsonNode merge(JsonNode target, JsonNode patch) {
        if (!patch.isObject()) {
            return patch;
        }
        ObjectNode result = target.isObject() ? (ObjectNode) target.deepCopy() : mapper.createObjectNode();
        for (Map.Entry<String, JsonNode> field : patch.properties()) {
            if (field.getValue().isNull()) {
                result.remove(field.getKey());
            } else {
                result.set(field.getKey(), merge(result.path(field.getKey()), field.getValue()));
            }
        }
        return result;
    }
}
