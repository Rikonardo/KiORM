package com.rikonardo.kiorm.queries;

import com.rikonardo.kiorm.serialization.DocumentSchema;

import java.util.List;

public abstract class AbstractQueryWhere extends AbstractQuery {
    public abstract List<Object> compileValues(DocumentSchema<?> schema);
}
