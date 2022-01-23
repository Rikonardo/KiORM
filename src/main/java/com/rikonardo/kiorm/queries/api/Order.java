package com.rikonardo.kiorm.queries.api;

import com.rikonardo.kiorm.queries.AbstractQueryOrder;
import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import com.rikonardo.kiorm.queries.parts.order.QueryOrderAsc;
import com.rikonardo.kiorm.queries.parts.order.QueryOrderDesc;
import com.rikonardo.kiorm.queries.parts.order.QueryOrderSeveral;
import com.rikonardo.kiorm.queries.parts.where.*;

import java.util.Arrays;
import java.util.List;

public class Order {
    public static AbstractQueryOrder asc(String field) {
        return new QueryOrderAsc(field);
    }
    public static AbstractQueryOrder desc(String field) {
        return new QueryOrderDesc(field);
    }
    public static AbstractQueryOrder several(AbstractQueryOrder... orders) {
        return new QueryOrderSeveral(Arrays.asList(orders));
    }
    public static AbstractQueryOrder several(List<AbstractQueryOrder> orders) {
        return new QueryOrderSeveral(orders);
    }
}
