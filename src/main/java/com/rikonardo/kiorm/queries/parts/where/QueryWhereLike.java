package com.rikonardo.kiorm.queries.parts.where;

import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class QueryWhereLike extends AbstractQueryWhere {
    private final String field;
    private final String template;

    @Override
    public String compile() {
        return "`" + field + "` LIKE ?";
    }

    @Override
    public List<Object> compileValues() {
        return Collections.singletonList(template);
    }
}
