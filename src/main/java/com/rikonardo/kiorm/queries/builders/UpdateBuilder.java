package com.rikonardo.kiorm.queries.builders;

import com.rikonardo.kiorm.KiORM;
import com.rikonardo.kiorm.exceptions.InvalidQueryException;
import com.rikonardo.kiorm.exceptions.RuntimeSQLException;
import com.rikonardo.kiorm.serialization.DocumentParser;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import com.rikonardo.kiorm.serialization.SupportedTypes;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateBuilder<T> {
    private final KiORM db;
    private final T target;
    private final DocumentParser.NameModifier tableNameModifier;
    private final DocumentParser.NameModifier fieldNameModifier;

    public UpdateBuilder(KiORM db, T target, DocumentParser.NameModifier tableNameModifier, DocumentParser.NameModifier fieldNameModifier) {
        this.db = db;
        this.target = target;
        this.tableNameModifier = tableNameModifier;
        this.fieldNameModifier = fieldNameModifier;
    }

    public int exec() {
        try {
            DocumentSchema<T> schema = (DocumentSchema<T>) DocumentParser.schema(this.target.getClass(), this.tableNameModifier, this.fieldNameModifier);
            Map<String, Object> fields = schema.toMapNoKeys(this.target);
            Map<String, Object> keys = schema.toMapKeys(this.target);
            String query = "UPDATE `" + schema.getTable() + "` SET " +
                    fields.keySet().stream().map(k -> "`" + k + "` = ?").collect(Collectors.joining(", ")) + " WHERE " +
                    keys.keySet().stream().map(k -> "`" + k + "` = ?").collect(Collectors.joining(", ")) + ";";

            PreparedStatement preparedStatement = db.getConnection().prepareStatement(query);
            int i = 1;
            for (String key : fields.keySet()) {
                SupportedTypes.SupportedType type = SupportedTypes.getFieldType(fields.get(key).getClass());
                if (type == null) throw new InvalidQueryException("Query contains value of unsupported type " + fields.get(key).getClass().getName());
                type.write(preparedStatement, i, fields.get(key));
                i++;
            }
            for (String key : keys.keySet()) {
                SupportedTypes.SupportedType type = SupportedTypes.getFieldType(keys.get(key).getClass());
                if (type == null) throw new InvalidQueryException("Query contains value of unsupported type " + keys.get(key).getClass().getName());
                type.write(preparedStatement, i, keys.get(key));
                i++;
            }

            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }
}
