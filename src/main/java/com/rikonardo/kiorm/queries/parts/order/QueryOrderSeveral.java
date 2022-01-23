package com.rikonardo.kiorm.queries.parts.order;

import com.rikonardo.kiorm.queries.AbstractQuery;
import com.rikonardo.kiorm.queries.AbstractQueryOrder;
import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class QueryOrderSeveral extends AbstractQueryOrder {
    private final List<AbstractQueryOrder> queries;

    @Override
    public String compile() {
        return queries.stream().map(AbstractQuery::compile).collect(Collectors.joining(", "));
    }
}
