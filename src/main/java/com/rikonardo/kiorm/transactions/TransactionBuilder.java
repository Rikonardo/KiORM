package com.rikonardo.kiorm.transactions;

import com.rikonardo.kiorm.KiORM;
import com.rikonardo.kiorm.exceptions.RuntimeSQLException;
import com.rikonardo.kiorm.queries.builders.*;
import com.rikonardo.kiorm.serialization.DocumentParser;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionBuilder {
    private final KiORM database;
    private final DocumentParser.NameModifier tableNameModifier;
    private final DocumentParser.NameModifier fieldNameModifier;

    private final List<TransactionUnit> units = new ArrayList<>();

    @Getter
    boolean running = false;

    public TransactionBuilder(KiORM database, DocumentParser.NameModifier tableNameModifier, DocumentParser.NameModifier fieldNameModifier) {
        this.database = database;
        this.tableNameModifier = tableNameModifier;
        this.fieldNameModifier = fieldNameModifier;
    }

    private <T extends QueryBuilder> T addUnit(T builder) {
        units.add(new TransactionUnit.QueryUnit(builder));
        return builder;
    }

    private PreparedStatement addUnit(PreparedStatement statement) {
        units.add(new TransactionUnit.StatementUnit(statement));
        return statement;
    }

    public <T> SelectBuilder<T> select(Class<T> target) {
        return addUnit(new SelectBuilder<>(database, target, this.tableNameModifier, this.fieldNameModifier, this));
    }


    public <T> SelectOneBuilder<T> selectOne(Class<T> target) {
        return addUnit(new SelectOneBuilder<>(database, target, this.tableNameModifier, this.fieldNameModifier, this));
    }

    public <T> CountBuilder<T> count(Class<T> target) {
        return addUnit(new CountBuilder<>(database, target, this.tableNameModifier, this.fieldNameModifier, this));
    }

    public <T> InsertBuilder<T> insert(T target) {
        return addUnit(new InsertBuilder<>(database, target, this.tableNameModifier, this.fieldNameModifier, this));
    }

    public <T> UpdateInstanceBuilder<T> update(T target) {
        return addUnit(new UpdateInstanceBuilder<>(database, target, this.tableNameModifier, this.fieldNameModifier, this));
    }

    public <T> UpdateBuilder<T> update(Class<T> target) {
        return addUnit(new UpdateBuilder<>(database, target, this.tableNameModifier, this.fieldNameModifier, this));
    }

    public <T> DeleteInstanceBuilder<T> delete(T target) {
        return addUnit(new DeleteInstanceBuilder<>(database, target, this.tableNameModifier, this.fieldNameModifier, this));
    }

    public <T> DeleteBuilder<T> delete(Class<T> target) {
        return addUnit(new DeleteBuilder<>(database, target, this.tableNameModifier, this.fieldNameModifier, this));
    }

    public PreparedStatement statement(String sql) throws SQLException {
        return addUnit(database.getConnection().prepareStatement(sql));
    }

    static abstract class TransactionUnit {
        @AllArgsConstructor
        static class QueryUnit extends TransactionUnit {
            private final QueryBuilder queryBuilder;
        }

        @AllArgsConstructor
        static class StatementUnit extends TransactionUnit {
            private final PreparedStatement statement;
        }

        public void execute() throws SQLException {
            if (this instanceof QueryUnit) {
                QueryUnit queryUnit = (QueryUnit) this;
                QueryBuilder queryBuilder = queryUnit.queryBuilder;
                if (queryBuilder instanceof QueryBuilder.ReturnsLong) {
                    ((QueryBuilder.ReturnsLong) queryBuilder).exec();
                } else if (queryBuilder instanceof QueryBuilder.ReturnsInt) {
                    ((QueryBuilder.ReturnsInt) queryBuilder).exec();
                } else if (queryBuilder instanceof QueryBuilder.ReturnsInstance) {
                    ((QueryBuilder.ReturnsInstance<?>) queryBuilder).exec();
                } else if (queryBuilder instanceof QueryBuilder.ReturnsList) {
                    ((QueryBuilder.ReturnsList) queryBuilder).exec();
                }
            } else if (this instanceof StatementUnit) {
                StatementUnit statementUnit = (StatementUnit) this;
                statementUnit.statement.execute();
            }
        }
    }

    public void commit() {
        this.running = true;
        try {
            database.getConnection().setAutoCommit(false);
            for (TransactionUnit unit : units) {
                unit.execute();
            }
            database.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
        this.running = false;
    }
}
