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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CountBuilder<T> implements QueryBuilder.ReturnsLong {
    private final KiORM db;
    private final Class<T> target;
    private final DocumentParser.NameModifier tableNameModifier;
    private final DocumentParser.NameModifier fieldNameModifier;

    private AbstractQueryWhere where;

    private final TransactionBuilder transaction;

    public CountBuilder(KiORM db, Class<T> target, DocumentParser.NameModifier tableNameModifier, DocumentParser.NameModifier fieldNameModifier, TransactionBuilder transaction) {
        this.db = db;
        this.target = target;
        this.tableNameModifier = tableNameModifier;
        this.fieldNameModifier = fieldNameModifier;
        this.transaction = transaction;
    }

    public CountBuilder<T> where(AbstractQueryWhere where) {
        this.where = where;
        return this;
    }

    public long exec() {
        if (transaction != null && !transaction.isRunning()) return 0;
        try {
            DocumentSchema<T> schema = DocumentParser.schema(this.target, this.tableNameModifier, this.fieldNameModifier);
            String query = "SELECT COUNT(*) FROM `" + schema.getTable() + "`";
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

            if (transaction != null) {
                preparedStatement.execute();
                return 0;
            }

            ResultSet rs = preparedStatement.executeQuery();

            long records = 0;
            if (rs.next())
                records = rs.getLong(1);
            return records;
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }
}
