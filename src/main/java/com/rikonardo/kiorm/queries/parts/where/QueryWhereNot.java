package com.rikonardo.kiorm.queries.parts.where;

import com.rikonardo.kiorm.queries.AbstractQuery;
import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class QueryWhereNot extends AbstractQueryWhere {
    private final AbstractQueryWhere query;

    @Override
    public String compile() {
        return "NOT (" + query.compile() + ")";
    }

    @Override
    public List<Object> compileValues() {
        return query.compileValues();
    }
}
