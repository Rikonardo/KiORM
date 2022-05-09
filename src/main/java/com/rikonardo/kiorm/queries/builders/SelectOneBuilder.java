package com.rikonardo.kiorm.queries.builders;

import com.rikonardo.kiorm.KiORM;
import com.rikonardo.kiorm.exceptions.InvalidQueryException;
import com.rikonardo.kiorm.exceptions.RuntimeSQLException;
import com.rikonardo.kiorm.queries.AbstractQueryOrder;
import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import com.rikonardo.kiorm.queries.parts.order.QueryOrderSeveral;
import com.rikonardo.kiorm.serialization.DocumentParser;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import com.rikonardo.kiorm.serialization.SupportedTypes;
import com.rikonardo.kiorm.transactions.TransactionBuilder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectOneBuilder<T> implements QueryBuilder.ReturnsInstance<T> {
    private final KiORM db;
    private final Class<T> target;
    private final DocumentParser.NameModifier tableNameModifier;
    private final DocumentParser.NameModifier fieldNameModifier;

    private AbstractQueryWhere where;
    private AbstractQueryOrder order;
    private int limitSkip = -1, limitCount = -1;

    private final TransactionBuilder transaction;

    public SelectOneBuilder(KiORM db, Class<T> target, DocumentParser.NameModifier tableNameModifier, DocumentParser.NameModifier fieldNameModifier, TransactionBuilder transaction) {
        this.db = db;
        this.target = target;
        this.tableNameModifier = tableNameModifier;
        this.fieldNameModifier = fieldNameModifier;
        this.transaction = transaction;
    }

    public SelectOneBuilder<T> where(AbstractQueryWhere where) {
        this.where = where;
        return this;
    }

    public SelectOneBuilder<T> order(AbstractQueryOrder order) {
        this.order = order;
        return this;
    }

    public SelectOneBuilder<T> order(AbstractQueryOrder... order) {
        this.order = new QueryOrderSeveral(Arrays.asList(order));
        return this;
    }

    public SelectOneBuilder<T> limit(int count) {
        this.limitSkip = -1;
        this.limitCount = count;
        return this;
    }

    public SelectOneBuilder<T> limit(int skip, int count) {
        this.limitSkip = skip;
        this.limitCount = count;
        return this;
    }

    public T exec() {
        if (transaction != null && !transaction.isRunning()) return null;
        try {
            DocumentSchema<T> schema = DocumentParser.schema(this.target, this.tableNameModifier, this.fieldNameModifier);
            String query = "SELECT * FROM `" + schema.getTable() + "`";
            List<Object> values = new ArrayList<>();
            if (this.where != null) {
                query += " WHERE " + this.where.compile(schema);
                values.addAll(this.where.compileValues(schema));
            }
            if (this.order != null) {
                query += " ORDER BY " + this.order.compile(schema);
            }
            if (this.limitSkip >= 0) {
                query += " LIMIT " + this.limitSkip + ", " + this.limitCount;
            } else if (this.limitCount >= 0) {
                query += " LIMIT " + this.limitCount;
            }
            query += ";";

            PreparedStatement preparedStatement = db.getConnection().prepareStatement(query);
            int i = 1;
            for (Object value : values) {
                SupportedTypes.SupportedType type = SupportedTypes.getAnyFieldType(value.getClass());
                if (type == null) throw new InvalidQueryException("Query contains value of unsupported type " + value.getClass().getName());
                type.write(preparedStatement, i, value);
                i++;
            }

            if (transaction != null) {
                preparedStatement.execute();
                return null;
            }

            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return schema.fromResultSet(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }
}
