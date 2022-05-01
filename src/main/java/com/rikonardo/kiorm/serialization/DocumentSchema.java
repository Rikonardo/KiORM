package com.rikonardo.kiorm.serialization;

import com.rikonardo.kiorm.exceptions.InvalidDocumentClassException;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentSchema<T> {
    @Getter private final Class<T> type;
    @Getter private final String table;
    @Getter private final List<DocumentParser.DocumentField> fields;
    @Getter private final DocumentParser.NameModifier tableNameModifier;
    @Getter private final DocumentParser.NameModifier fieldNameModifier;

    public DocumentSchema(Class<T> type, String table, List<DocumentParser.DocumentField> fields, DocumentParser.NameModifier tableNameModifier, DocumentParser.NameModifier fieldNameModifier) {
        this.type = type;
        this.table = table;
        this.fields = fields;
        this.tableNameModifier = tableNameModifier;
        this.fieldNameModifier = fieldNameModifier;
    }

    public T fromResultSet(ResultSet rs) throws SQLException {
        try {
            T instance = type.getConstructor().newInstance();
            for (DocumentParser.DocumentField field : fields)
                field.write(instance, field.getSerializer().deserialize(field.getSerializer().getStorageType().read(rs, field.getName())));
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new InvalidDocumentClassException("Document class must have no-args constructor in order to be used in select operations");
        }
    }

    public Map<String, Object> mapWithoutId(T instance) {
        try {
            Map<String, Object> map = new HashMap<>();
            for (DocumentParser.DocumentField field : fields)
                if (!field.isAutoIncrement())
                    map.put(field.getName(), field.getSerializer().serialize(field.read(instance)));
            return map;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void fillKey(ResultSet rs, T instance) throws SQLException {
        try {
            for (DocumentParser.DocumentField field : fields) {
                if (field.isPrimaryKey()) {
                    field.write(instance, field.getSerializer().deserialize(field.getSerializer().getStorageType().read(rs, 1)));
                    return;
                }
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> mapKeysOnly(T instance) {
        try {
            Map<String, Object> map = new HashMap<>();
            for (DocumentParser.DocumentField field : fields)
                if (field.isPrimaryKey())
                    map.put(field.getName(), field.getSerializer().serialize(field.read(instance)));
            return map;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public SupportedTypes.SupportedType getFieldType(String name) {
        for (DocumentParser.DocumentField field : fields)
            if (field.getName().equals(name))
                return field.getSerializer().getStorageType();
        return null;
    }

    public String toStorageFieldName(String name) {
        return fieldNameModifier == null ? name : fieldNameModifier.method(name, type);
    }

    public Object toStorageFieldValue(String name, Object value) {
        String fieldName = toStorageFieldName(name);
        for (DocumentParser.DocumentField field : fields)
            if (field.getName().equals(fieldName))
                return field.getSerializer().serialize(value);
        return value;
    }
}
