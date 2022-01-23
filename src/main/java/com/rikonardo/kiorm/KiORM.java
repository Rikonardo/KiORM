package com.rikonardo.kiorm;

import com.rikonardo.kiorm.exceptions.RuntimeSQLException;
import com.rikonardo.kiorm.queries.builders.*;
import com.rikonardo.kiorm.serialization.DocumentParser;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
public class KiORM {
    @Getter private Connection connection;
    @Setter private DocumentParser.NameModifier tableNameModifier = null;
    @Setter private DocumentParser.NameModifier fieldNameModifier = null;

    public KiORM(String jdbcString) throws SQLException {
        this.connection = DriverManager.getConnection(jdbcString);
    }

    public void setConnectionUrl(String jdbcString) throws SQLException {
        if (this.connection != null) this.connection.close();
        this.connection = DriverManager.getConnection(jdbcString);
    }

    public void close() throws SQLException {
        connection.close();
    }

    public <T> SelectBuilder<T> select(Class<T> target) {
        return new SelectBuilder<>(this, target, this.tableNameModifier, this.fieldNameModifier);
    }
    public <T> CountBuilder<T> count(Class<T> target) {
        return new CountBuilder<>(this, target, this.tableNameModifier, this.fieldNameModifier);
    }
    public <T> InsertBuilder<T> insert(T target) {
        return new InsertBuilder<>(this, target, this.tableNameModifier, this.fieldNameModifier);
    }
    public <T> UpdateBuilder<T> update(T target) {
        return new UpdateBuilder<>(this, target, this.tableNameModifier, this.fieldNameModifier);
    }
    public <T> DeleteBuilder<T> delete(T target) {
        return new DeleteBuilder<>(this, target, this.tableNameModifier, this.fieldNameModifier);
    }

    public <T> boolean checkIfTableExists(Class<T> target) {
        try {
            DocumentSchema<T> schema = DocumentParser.schema(target, this.tableNameModifier, this.fieldNameModifier);
            String query = "SELECT count(*) FROM information_schema.tables WHERE table_name = '" + schema.getTable() + "' LIMIT 1;";
            PreparedStatement preparedStatement = this.connection.prepareStatement(query);
            ResultSet rs = preparedStatement.executeQuery();
            int records = 0;
            if (rs.next())
                records = rs.getInt(1);
            return records > 0;
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }
    public <T> void createTable(Class<T> target) {
        try {
            DocumentSchema<T> schema = DocumentParser.schema(target, this.tableNameModifier, this.fieldNameModifier);
            List<String> keys = new ArrayList<>();
            String query = "CREATE TABLE `" + schema.getTable() + "` (" +
                    schema.getFields().stream().map(field -> {
                        if (field.isPrimaryKey()) keys.add(field.getName());
                        return "`" + field.getName() + "` " + field.getSerializer().getStorageType().getSqlName() + " NOT NULL" + (field.isAutoIncrement() ? " AUTO_INCREMENT" : "");
                    }).collect(Collectors.joining(", ")) + (keys.size() > 0 ? ", PRIMARY KEY (" + keys.stream().map(k -> "`" + k + "`").collect(Collectors.joining(", ")) + ")" : "") + ");";
            this.connection.prepareStatement(query).executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }
    public <T> void createTableIfNotExist(Class<T> target) {
        if(!this.checkIfTableExists(target))
            createTable(target);
    }

    public static void clearSchemaCache() {
        DocumentParser.getCache().clear();
    }
}
