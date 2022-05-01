package com.rikonardo.kiorm.queries.parts.order;

import com.rikonardo.kiorm.queries.AbstractQueryOrder;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class QueryOrderDesc extends AbstractQueryOrder {
    private final String field;

    @Override
    public String compile(DocumentSchema<?> schema) {
        return "`" + schema.toStorageFieldName(field) + "` DESC";
    }
}
