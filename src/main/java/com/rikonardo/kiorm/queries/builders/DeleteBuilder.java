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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeleteBuilder<T> {
    private final KiORM db;
    private final Class<T> target;
    private final DocumentParser.NameModifier tableNameModifier;
    private final DocumentParser.NameModifier fieldNameModifier;

    private AbstractQueryWhere where;

    public DeleteBuilder(KiORM db, Class<T> target, DocumentParser.NameModifier tableNameModifier, DocumentParser.NameModifier fieldNameModifier) {
        this.db = db;
        this.target = target;
        this.tableNameModifier = tableNameModifier;
        this.fieldNameModifier = fieldNameModifier;
    }

    public DeleteBuilder<T> where(AbstractQueryWhere where) {
        this.where = where;
        return this;
    }

    public int exec() {
        try {
            DocumentSchema<T> schema = DocumentParser.schema(this.target, this.tableNameModifier, this.fieldNameModifier);
            String query = "DELETE FROM `" + schema.getTable() + "`";
            List<Object> values = new ArrayList<>();
            if (this.where != null) {
                query += " WHERE " + this.where.compile(schema);
                values.addAll(this.where.compileValues(schema));
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

            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }
}
