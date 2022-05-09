package com.rikonardo.kiorm.queries.builders;

import java.util.List;

public interface QueryBuilder {
    interface ReturnsList extends QueryBuilder {
        List<?> exec();
    }
    interface ReturnsLong extends QueryBuilder {
        long exec();
    }
    interface ReturnsInt extends QueryBuilder {
        int exec();
    }
    interface ReturnsInstance<T> extends QueryBuilder {
        T exec();
    }
}
