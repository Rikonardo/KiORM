package com.rikonardo.kiorm.queries.api;

import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import com.rikonardo.kiorm.queries.parts.where.*;

import java.util.Arrays;
import java.util.List;

public class Where {
    public static AbstractQueryWhere eq(String field, Object value) {
        return new QueryWhereEquals(field, value);
    }
    public static AbstractQueryWhere neq(String field, Object value) {
        return new QueryWhereNotEquals(field, value);
    }
    public static AbstractQueryWhere gt(String field, Object value) {
        return new QueryWhereGreaterThan(field, value);
    }
    public static AbstractQueryWhere lt(String field, Object value) {
        return new QueryWhereLessThan(field, value);
    }
    public static AbstractQueryWhere gte(String field, Object value) {
        return new QueryWhereGreaterThanOrEquals(field, value);
    }
    public static AbstractQueryWhere lte(String field, Object value) {
        return new QueryWhereLessThanOrEquals(field, value);
    }
    public static AbstractQueryWhere and(AbstractQueryWhere... queries) {
        return new QueryWhereAnd(Arrays.asList(queries));
    }
    public static AbstractQueryWhere or(AbstractQueryWhere... queries) {
        return new QueryWhereOr(Arrays.asList(queries));
    }
    public static AbstractQueryWhere not(AbstractQueryWhere query) {
        return new QueryWhereNot(query);
    }
    public static AbstractQueryWhere in(String field, Object... values) {
        return new QueryWhereIn(field, Arrays.asList(values));
    }
    public static AbstractQueryWhere in(String field, List<Object> values) {
        return new QueryWhereIn(field, values);
    }
    public static AbstractQueryWhere between(String field, Object a, Object b) {
        return new QueryWhereBetween(field, a, b);
    }
    public static AbstractQueryWhere like(String field, String template) {
        return new QueryWhereLike(field, template);
    }
}
