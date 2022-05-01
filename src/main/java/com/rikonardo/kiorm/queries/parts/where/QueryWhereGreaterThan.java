package com.rikonardo.kiorm.queries.parts.where;

import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class QueryWhereGreaterThan extends AbstractQueryWhere {
    private final String field;
    private final Object value;

    @Override
    public String compile(DocumentSchema<?> schema) {
        return "`" + schema.toStorageFieldName(field) + "` > ?";
    }

    @Override
    public List<Object> compileValues(DocumentSchema<?> schema) {
        return Collections.singletonList(schema.toStorageFieldValue(field, value));
    }
}
