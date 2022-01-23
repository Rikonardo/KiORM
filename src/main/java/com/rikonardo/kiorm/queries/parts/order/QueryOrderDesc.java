package com.rikonardo.kiorm.queries.parts.order;

import com.rikonardo.kiorm.queries.AbstractQueryOrder;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class QueryOrderDesc extends AbstractQueryOrder {
    private final String field;

    @Override
    public String compile() {
        return "`" + field + "` DESC";
    }
}
