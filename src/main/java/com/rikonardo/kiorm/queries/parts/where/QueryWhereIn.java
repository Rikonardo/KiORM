package com.rikonardo.kiorm.queries.parts.where;

import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class QueryWhereIn extends AbstractQueryWhere {
    private final String field;
    private final List<Object> values;

    @Override
    public String compile(DocumentSchema<?> schema) {
        return "`" + schema.toStorageFieldName(field) + "` IN (" + values.stream().map((val) -> "?").collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public List<Object> compileValues(DocumentSchema<?> schema) {
        return values.stream().map((val) -> schema.toStorageFieldValue(field, val)).collect(Collectors.toList());
    }
}
