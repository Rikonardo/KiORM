package com.rikonardo.kiorm.queries.builders;

import com.rikonardo.kiorm.KiORM;
import com.rikonardo.kiorm.exceptions.InvalidDocumentClassException;
import com.rikonardo.kiorm.exceptions.InvalidQueryException;
import com.rikonardo.kiorm.exceptions.RuntimeSQLException;
import com.rikonardo.kiorm.serialization.DocumentParser;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import com.rikonardo.kiorm.serialization.SupportedTypes;
import com.rikonardo.kiorm.transactions.TransactionBuilder;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;

public class DeleteInstanceBuilder<T> implements QueryBuilder.ReturnsInt {
    private final KiORM db;
    private final T target;
    private final DocumentParser.NameModifier tableNameModifier;
    private final DocumentParser.NameModifier fieldNameModifier;

    private final TransactionBuilder transaction;

    public DeleteInstanceBuilder(KiORM db, T target, DocumentParser.NameModifier tableNameModifier, DocumentParser.NameModifier fieldNameModifier, TransactionBuilder transaction) {
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
            Map<String, Object> keys = schema.mapKeysOnly(this.target);
            if (keys.size() == 0)
                throw new InvalidDocumentClassException("Document must have primary key in order to be used in delete operations");
            String query = "DELETE FROM `" + schema.getTable() + "` WHERE " +
                    keys.keySet().stream().map(k -> "`" + k + "` = ?").collect(Collectors.joining(", ")) + ";";

            PreparedStatement preparedStatement = db.getConnection().prepareStatement(query);
            int i = 1;
            for (String key : keys.keySet()) {
                SupportedTypes.SupportedType type = schema.getFieldType(key);
                if (type == null) throw new InvalidQueryException("Query contains value of unsupported type " + keys.get(key).getClass().getName());
                type.write(preparedStatement, i, keys.get(key));
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
