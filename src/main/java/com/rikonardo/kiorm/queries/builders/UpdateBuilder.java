package com.rikonardo.kiorm.queries.builders;

import com.rikonardo.kiorm.KiORM;
import com.rikonardo.kiorm.exceptions.InvalidQueryException;
import com.rikonardo.kiorm.exceptions.RuntimeSQLException;
import com.rikonardo.kiorm.queries.AbstractQueryWhere;
import com.rikonardo.kiorm.serialization.DocumentParser;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import com.rikonardo.kiorm.serialization.SupportedTypes;
import com.rikonardo.kiorm.transactions.TransactionBuilder;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateBuilder<T> implements QueryBuilder.ReturnsInt {
    private final KiORM db;
    private final Class<T> target;
    private final DocumentParser.NameModifier tableNameModifier;
    private final DocumentParser.NameModifier fieldNameModifier;

    private final Map<String, Object> fields;
    private AbstractQueryWhere where;

    private final TransactionBuilder transaction;

    public UpdateBuilder(KiORM db, Class<T> target, DocumentParser.NameModifier tableNameModifier, DocumentParser.NameModifier fieldNameModifier, TransactionBuilder transaction) {
        this.db = db;
        this.target = target;
        this.tableNameModifier = tableNameModifier;
        this.fieldNameModifier = fieldNameModifier;
        this.fields = new HashMap<>();
        this.transaction = transaction;
    }

    public UpdateBuilder<T> where(AbstractQueryWhere where) {
        this.where = where;
        return this;
    }

    public UpdateBuilder<T> set(String fieldName, Object value) {
        fields.put(fieldName, value);
        return this;
    }

    public int exec() {
        if (transaction != null && !transaction.isRunning()) return 0;
        try {
            DocumentSchema<T> schema = DocumentParser.schema(this.target, this.tableNameModifier, this.fieldNameModifier);
            List<Object> values = new ArrayList<>();
            String query = "UPDATE `" + schema.getTable() + "` SET " +
                    fields.keySet().stream().map(k -> { values.add(schema.toStorageFieldValue(k, fields.get(k))); return "`" + schema.toStorageFieldName(k) + "` = ?"; }).collect(Collectors.joining(", "));

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

            if (transaction != null) {
                preparedStatement.execute();
                return 0;
            }

            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }
}
