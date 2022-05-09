package com.rikonardo.kiorm;

import com.rikonardo.kiorm.exceptions.RuntimeSQLException;
import com.rikonardo.kiorm.queries.builders.*;
import com.rikonardo.kiorm.serialization.DocumentParser;
import com.rikonardo.kiorm.serialization.DocumentSchema;
import com.rikonardo.kiorm.transactions.TransactionBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.rikonardo.kiorm.annotations.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
public class KiORM {
    /**
     * JDBC connection object.
     * Can be null, if this instance was created by empty constructor and {@link #connect} method haven't called.
     */
    @Getter private Connection connection;

    /**
     * Table name modifier is a lambda, that called every time document class parsing happens.
     * It's return value will be used as a table name, instead of name, specified in {@link Document} annotation.
     */
    @Setter private DocumentParser.NameModifier tableNameModifier = null;

    /**
     * Field name modifier is a lambda, that called every time document class parsing happens.
     * It's return value will be used as a field name, instead of name, specified in {@link Field} annotation.
     */
    @Setter private DocumentParser.NameModifier fieldNameModifier = null;

    /**
     * Creates instance of KiORM and immediately connects to the database.
     * @param jdbcString JDBC connection string
     * @throws RuntimeSQLException If unable to connect to database
     */
    public KiORM(String jdbcString) {
        this.connect(jdbcString);
    }

    /**
     * Connects to the database.
     * @param jdbcString JDBC connection string
     * @throws RuntimeSQLException If unable to connect to database
     */
    public void connect(String jdbcString) {
        try {
            if (this.connection != null) this.connection.close();
            this.connection = DriverManager.getConnection(jdbcString);
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }

    /**
     * Closes database connection
     * @throws RuntimeSQLException If a database access error occurs
     */
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }

    /**
     * Creates SelectBuilder to prepare {@code SELECT} query.
     * @param target Document class
     * @return {@code SELECT} query builder
     */
    public <T> SelectBuilder<T> select(Class<T> target) {
        return new SelectBuilder<>(this, target, this.tableNameModifier, this.fieldNameModifier, null);
    }

    /**
     * Creates SelectOneBuilder to prepare {@code SELECT} query that selects only first match or null.
     * @param target Document class
     * @return {@code SELECT} query builder
     */
    public <T> SelectOneBuilder<T> selectOne(Class<T> target) {
        return new SelectOneBuilder<>(this, target, this.tableNameModifier, this.fieldNameModifier, null);
    }

    /**
     * Creates CountBuilder to prepare {@code SELECT COUNT(*)} query.
     * @param target Document class
     * @return {@code SELECT COUNT(*)} query builder
     */
    public <T> CountBuilder<T> count(Class<T> target) {
        return new CountBuilder<>(this, target, this.tableNameModifier, this.fieldNameModifier, null);
    }

    /**
     * Creates InsertBuilder to prepare {@code INSERT} query.
     * @param target Document instance
     * @return {@code INSERT} query builder
     */
    public <T> InsertBuilder<T> insert(T target) {
        return new InsertBuilder<>(this, target, this.tableNameModifier, this.fieldNameModifier, null);
    }

    /**
     * Creates UpdateBuilder to prepare {@code UPDATE} query.
     * @param target Document instance
     * @return {@code UPDATE} query builder
     */
    public <T> UpdateInstanceBuilder<T> update(T target) {
        return new UpdateInstanceBuilder<>(this, target, this.tableNameModifier, this.fieldNameModifier, null);
    }

    /**
     * Creates UpdateBuilder to prepare {@code UPDATE} query.
     * @param target Document class
     * @return {@code UPDATE} query builder
     */
    public <T> UpdateBuilder<T> update(Class<T> target) {
        return new UpdateBuilder<>(this, target, this.tableNameModifier, this.fieldNameModifier, null);
    }

    /**
     * Creates DeleteInstanceBuilder to prepare {@code DELETE} query.
     * @param target Document instance
     * @return {@code DELETE} query builder
     */
    public <T> DeleteInstanceBuilder<T> delete(T target) {
        return new DeleteInstanceBuilder<>(this, target, this.tableNameModifier, this.fieldNameModifier, null);
    }

    /**
     * Creates DeleteBuilder to prepare {@code DELETE} query.
     * @param target Document class
     * @return {@code DELETE} query builder
     */
    public <T> DeleteBuilder<T> delete(Class<T> target) {
        return new DeleteBuilder<>(this, target, this.tableNameModifier, this.fieldNameModifier, null);
    }

    /**
     * Creates TransactionBuilder.
     * @return Transaction builder
     */
    public TransactionBuilder transaction() {
        return new TransactionBuilder(this, this.tableNameModifier, this.fieldNameModifier);
    }

    /**
     * Checks if table for specified document class exists.
     * @param target Document class
     * @return true if table with this name was found in the database
     */
    public <T> boolean checkIfTableExists(Class<T> target) {
        try {
            DocumentSchema<T> schema = DocumentParser.schema(target, this.tableNameModifier, this.fieldNameModifier);
            String query = "SELECT count(*) FROM information_schema.tables WHERE table_name = '" + schema.getTable() + "' AND table_schema = '" + connection.getCatalog() + "' LIMIT 1;";
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

    /**
     * Creates table for specified document class.
     * @param target Document class
     */
    public <T> void createTable(Class<T> target) {
        try {
            DocumentSchema<T> schema = DocumentParser.schema(target, this.tableNameModifier, this.fieldNameModifier);
            List<String> keys = new ArrayList<>();
            List<String> singleUniques = new ArrayList<>();
            Map<String, List<String>> groupUniques = new HashMap<>();
            for (DocumentParser.DocumentField field : schema.getFields()) {
                if (field.getUniqueKey() == null) continue;
                if (field.getUniqueKey().isEmpty()) {
                    singleUniques.add(field.getName());
                } else {
                    if (!groupUniques.containsKey(field.getUniqueKey()))
                        groupUniques.put(field.getUniqueKey(), new ArrayList<>());
                    groupUniques.get(field.getUniqueKey()).add(field.getName());
                }
            }
            String query = "CREATE TABLE `" + schema.getTable() + "` (" +
                    schema.getFields().stream().map(field -> {
                        if (field.isPrimaryKey()) keys.add(field.getName());
                        return "`" + field.getName() + "` " + field.getSerializer().getStorageType().getSqlName() + " NOT NULL" + (field.isAutoIncrement() ? " AUTO_INCREMENT" : "");
                    }).collect(Collectors.joining(", "))
                    + (keys.size() > 0 ? ", PRIMARY KEY (" + keys.stream().map(k -> "`" + k + "`").collect(Collectors.joining(", ")) + ")" : "")
                    + (singleUniques.size() > 0 ? ", UNIQUE (" + singleUniques.stream().map(k -> "`" + k + "`").collect(Collectors.joining(", ")) + ")" : "")
                    + groupUniques.keySet().stream().map(key -> {
                        List<String> uniques = groupUniques.get(key);
                        return ", CONSTRAINT `" + key + "` UNIQUE (" + uniques.stream().map(k -> "`" + k + "`").collect(Collectors.joining(", ")) + ")";
                    }).collect(Collectors.joining(""))
                    + ");";
            this.connection.prepareStatement(query).executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }

    /**
     * Checks if table for specified document class exists. If it doesn't - creates it.
     * @param target Document class
     */
    public <T> void createTableIfNotExist(Class<T> target) {
        if(!this.checkIfTableExists(target))
            createTable(target);
    }

    /**
     * Clears schemas cache. Once document class parsed, its schema is added to cache.
     * You can disable cache completely by setting java property {@code "kiorm.disableDocumentSchemaCache"} to {@code "false"}.
     */
    public static void clearSchemaCache() {
        DocumentParser.getCache().clear();
    }
}
