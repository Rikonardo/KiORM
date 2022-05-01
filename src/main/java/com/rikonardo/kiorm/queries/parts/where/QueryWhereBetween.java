package com.rikonardo.kiorm.queries.parts.where;

import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class QueryWhereBetween extends AbstractQueryWhere {
    private final String field;
    private final Object a;
    private final Object b;

    @Override
    public String compile(DocumentSchema<?> schema) {
        return "`" + schema.toStorageFieldName(field) + "` BETWEEN ? AND ?";
    }

    @Override
    public List<Object> compileValues(DocumentSchema<?> schema) {
        return Arrays.asList(schema.toStorageFieldValue(field, a), schema.toStorageFieldValue(field, b));
    }
}
