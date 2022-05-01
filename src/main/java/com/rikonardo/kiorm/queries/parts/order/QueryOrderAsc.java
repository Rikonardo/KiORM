package com.rikonardo.kiorm.queries.parts.order;

import com.rikonardo.kiorm.queries.AbstractQuery;
import com.rikonardo.kiorm.queries.AbstractQueryOrder;
import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class QueryOrderAsc extends AbstractQueryOrder {
    private final String field;

    @Override
    public String compile(DocumentSchema<?> schema) {
        return "`" + schema.toStorageFieldName(field) + "` ASC";
    }
}
