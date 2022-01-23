package com.rikonardo.kiorm.queries.parts.where;

import com.rikonardo.kiorm.queries.AbstractQuery;
import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class QueryWhereOr extends AbstractQueryWhere {
    private final List<AbstractQueryWhere> queries;

    @Override
    public String compile() {
        return "(" + queries.stream().map(AbstractQuery::compile).collect(Collectors.joining(") OR (")) + ")";
    }

    @Override
    public List<Object> compileValues() {
        List<Object> values = new ArrayList<>();
        for (AbstractQueryWhere q : queries)
            values.addAll(q.compileValues());
        return values;
    }
}
