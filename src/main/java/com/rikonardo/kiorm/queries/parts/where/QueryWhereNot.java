package com.rikonardo.kiorm.queries.parts.where;

import com.rikonardo.kiorm.queries.AbstractQuery;
import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class QueryWhereNot extends AbstractQueryWhere {
    private final AbstractQueryWhere query;

    @Override
    public String compile(DocumentSchema<?> schema) {
        return "NOT (" + query.compile(schema) + ")";
    }

    @Override
    public List<Object> compileValues(DocumentSchema<?> schema) {
        return query.compileValues(schema);
    }
}
