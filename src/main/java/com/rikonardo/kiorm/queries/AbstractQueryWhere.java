package com.rikonardo.kiorm.queries;

import java.util.List;

public abstract class AbstractQueryWhere extends AbstractQuery {
    public abstract List<Object> compileValues();
}
