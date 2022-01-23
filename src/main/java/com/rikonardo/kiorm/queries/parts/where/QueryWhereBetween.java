package com.rikonardo.kiorm.queries.parts.where;

import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class QueryWhereBetween extends AbstractQueryWhere {
    private final String field;
    private final Object a;
    private final Object b;

    @Override
    public String compile() {
        return "`" + field + "` BETWEEN ? AND ?";
    }

    @Override
    public List<Object> compileValues() {
        return Arrays.asList(a, b);
    }
}
