package com.rikonardo.kiorm.queries.parts.order;

import com.rikonardo.kiorm.queries.AbstractQuery;
import com.rikonardo.kiorm.queries.AbstractQueryOrder;
import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class QueryOrderSeveral extends AbstractQueryOrder {
    private final List<AbstractQueryOrder> queries;

    @Override
    public String compile(DocumentSchema<?> schema) {
        return queries.stream().map(el -> el.compile(schema)).collect(Collectors.joining(", "));
    }
}
