package com.rikonardo.kiorm.queries;

import com.rikonardo.kiorm.serialization.DocumentSchema;

public abstract class AbstractQuery {
    public abstract String compile(DocumentSchema<?> schema);
}
