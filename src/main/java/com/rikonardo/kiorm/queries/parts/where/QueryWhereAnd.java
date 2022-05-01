package com.rikonardo.kiorm.queries.parts.where;

import com.rikonardo.kiorm.queries.AbstractQuery;
import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class QueryWhereAnd extends AbstractQueryWhere {
    private final List<AbstractQueryWhere> queries;

    @Override
    public String compile(DocumentSchema<?> schema) {
        return "(" + queries.stream().map(el -> el.compile(schema)).collect(Collectors.joining(") AND (")) + ")";
    }

    @Override
    public List<Object> compileValues(DocumentSchema<?> schema) {
        List<Object> values = new ArrayList<>();
        for (AbstractQueryWhere q : queries)
            values.addAll(q.compileValues(schema));
        return values;
    }
}
