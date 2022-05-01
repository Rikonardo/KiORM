package com.rikonardo.kiorm.queries.parts.where;

import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class QueryWhereLike extends AbstractQueryWhere {
    private final String field;
    private final String template;

    @Override
    public String compile(DocumentSchema<?> schema) {
        return "`" + schema.toStorageFieldName(field) + "` LIKE ?";
    }

    @Override
    public List<Object> compileValues(DocumentSchema<?> schema) {
        return Collections.singletonList(template);
    }
}
