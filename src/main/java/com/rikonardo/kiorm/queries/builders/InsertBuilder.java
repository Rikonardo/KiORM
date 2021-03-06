package com.rikonardo.kiorm.queries.builders;

import com.rikonardo.kiorm.KiORM;
import com.rikonardo.kiorm.exceptions.InvalidQueryException;
import com.rikonardo.kiorm.exceptions.RuntimeSQLException;
import com.rikonardo.kiorm.serialization.DocumentParser;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import com.rikonardo.kiorm.serialization.SupportedTypes;
import com.rikonardo.kiorm.transactions.TransactionBuilder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.stream.Collectors;

public class InsertBuilder<T> implements QueryBuilder.ReturnsInt {
    private final KiORM db;
    private final T target;
    private final DocumentParser.NameModifier tableNameModifier;
    private final DocumentParser.NameModifier fieldNameModifier;

    private final TransactionBuilder transaction;

    public InsertBuilder(KiORM db, T target, DocumentParser.NameModifier tableNameModifier, DocumentParser.NameModifier fieldNameModifier, TransactionBuilder transaction) {
        this.db = db;
        this.target = target;
        this.tableNameModifier = tableNameModifier;
        this.fieldNameModifier = fieldNameModifier;
        this.transaction = transaction;
    }

    public int exec() {
        if (transaction != null && !transaction.isRunning()) return 0;
        try {
            DocumentSchema<T> schema = (DocumentSchema<T>) DocumentParser.schema(this.target.getClass(), this.tableNameModifier, this.fieldNameModifier);
            Map<String, Object> fields = schema.mapWithoutId(this.target);
            String query = "INSERT INTO `" + schema.getTable() + "` (" +
                    fields.keySet().stream().map(k -> "`" + k + "`").collect(Collectors.joining(", ")) + ") VALUES (" +
                    fields.keySet().stream().map(k -> "?").collect(Collectors.joining(", ")) + ");";

            PreparedStatement preparedStatement = db.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            for (String key : fields.keySet()) {
                SupportedTypes.SupportedType type = schema.getFieldType(key);
                if (type == null) throw new InvalidQueryException("Query contains value of unsupported type " + fields.get(key).getClass().getName());
                type.write(preparedStatement, i, fields.get(key));
                i++;
            }

            if (transaction != null) {
                preparedStatement.execute();
                return 0;
            }

            int rows = preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if(generatedKeys.next()) {
                    schema.fillKey(generatedKeys, this.target);
                }
            }

            return rows;
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }
}
