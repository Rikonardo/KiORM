package com.rikonardo.kiorm.queries.parts.where;

import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class QueryWhereLessThanOrEquals extends AbstractQueryWhere {
    private final String field;
    private final Object value;

    @Override
    public String compile() {
        return "`" + field + "` <= ?";
    }

    @Override
    public List<Object> compileValues() {
        return Collections.singletonList(value);
    }
}
