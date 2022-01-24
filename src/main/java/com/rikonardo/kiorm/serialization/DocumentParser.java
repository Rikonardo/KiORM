package com.rikonardo.kiorm.serialization;

import com.rikonardo.kiorm.annotations.*;
import com.rikonardo.kiorm.exceptions.InvalidDocumentClassException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@UtilityClass
public class DocumentParser {
    @Getter private static final List<CachedDocumentSchema<?>> cache = new ArrayList<>();

    public static <T> DocumentSchema<T> schema(Class<T> clazz) {
        return schema(clazz, null, null);
    }

    public static <T> DocumentSchema<T> schema(Class<T> clazz, DocumentParser.NameModifier tableNameModifier, DocumentParser.NameModifier fieldNameModifier) {
        String disableCacheProperty = System.getProperty("kiorm.disableDocumentSchemaCache");
        boolean cacheEnabled = disableCacheProperty == null || !disableCacheProperty.equalsIgnoreCase("true");
        if(cacheEnabled)
            for (CachedDocumentSchema<?> s : cache)
                if (s.equals(clazz, tableNameModifier, fieldNameModifier))
                    return (DocumentSchema<T>) s.getSchema();

        Document docInfo = clazz.getAnnotation(Document.class);
        if (docInfo == null) throw new InvalidDocumentClassException("Class " + clazz.getName() + " does not have @Document annotation");
        String tableName = tableNameModifier == null ? docInfo.value() : tableNameModifier.method(docInfo.value(), clazz);

        List<DocumentField> fields = new ArrayList<>();

        Field[] classFields = clazz.getDeclaredFields();
        for (Field classField : classFields) {
            com.rikonardo.kiorm.annotations.Field fieldInfo = classField.getAnnotation(com.rikonardo.kiorm.annotations.Field.class);
            if (fieldInfo == null) continue;
            classField.setAccessible(true);
            String fieldName = fieldNameModifier == null ? fieldInfo.value() : fieldNameModifier.method(fieldInfo.value(), clazz);

            if (findField(fields, fieldName) != null) throw new InvalidDocumentClassException("Field " + fieldName + " specified twice");

            boolean isPrimaryKey = classField.getAnnotation(PrimaryKey.class) != null;
            boolean isAutoIncrement = classField.getAnnotation(AutoIncrement.class) != null;
            if (isAutoIncrement && !isPrimaryKey) throw new InvalidDocumentClassException("@AutoIncrement used without @PrimaryKey at field " + fieldName);
            fields.add(new StandardDocumentField(fieldName, new SerializerMiddleware(classField.getType(), classField.getAnnotation(Serializer.class), classField.getAnnotation(Fixed.class)), isPrimaryKey, isAutoIncrement, classField));
        }

        Method[] classMethods = clazz.getDeclaredMethods();
        for (Method classMethod : classMethods) {
            com.rikonardo.kiorm.annotations.Field fieldInfo = classMethod.getAnnotation(com.rikonardo.kiorm.annotations.Field.class);
            if (fieldInfo == null) continue;
            classMethod.setAccessible(true);
            String fieldName = fieldNameModifier == null ? fieldInfo.value() : fieldNameModifier.method(fieldInfo.value(), clazz);

            boolean isSetter = classMethod.getReturnType() == void.class;
            Class<?> type;
            if (isSetter) {
                Class<?>[] params = classMethod.getParameterTypes();
                if (params.length != 1) throw new InvalidDocumentClassException("Setter field must contain 1 param");
                type = params[0];
            } else {
                if (classMethod.getParameterTypes().length != 0) throw new InvalidDocumentClassException("Getter field must not contain params");
                type = classMethod.getReturnType();
            }

            DocumentField field = findField(fields, fieldName);
            if (field == null) {
                boolean isPrimaryKey = classMethod.getAnnotation(PrimaryKey.class) != null;
                boolean isAutoIncrement = classMethod.getAnnotation(AutoIncrement.class) != null;
                ComputedDocumentField cfield = new ComputedDocumentField(fieldName, new SerializerMiddleware(type, classMethod.getAnnotation(Serializer.class), classMethod.getAnnotation(Fixed.class)), isPrimaryKey, isAutoIncrement);
                if (isSetter) cfield.setSetter(classMethod);
                else cfield.setGetter(classMethod);
                fields.add(cfield);
            } else {
                if (!(field instanceof ComputedDocumentField)) throw new InvalidDocumentClassException("Field " + fieldName + " specified twice");
                if (field.getSerializer().getRealType() != type) throw new InvalidDocumentClassException("Getter/Setter type mismatch in field " + fieldName);
                ComputedDocumentField cfield = (ComputedDocumentField) field;
                if (isSetter) {
                    if (cfield.getSetter() != null) throw new InvalidDocumentClassException("Setter for field " + fieldName + " specified twice");
                    cfield.setSetter(classMethod);
                }
                else {
                    if (cfield.getGetter() != null) throw new InvalidDocumentClassException("Getter for field " + fieldName + " specified twice");
                    cfield.setGetter(classMethod);
                }
                Serializer serializer = classMethod.getAnnotation(Serializer.class);
                Fixed fixed = classMethod.getAnnotation(Fixed.class);
                if (
                    !cfield.getSerializer().hasSerializer() && serializer != null ||
                    !cfield.getSerializer().getStorageType().isFixed() && fixed != null
                ) {
                    cfield.setSerializer(new SerializerMiddleware(type, serializer != null ? serializer.value() : cfield.getSerializer().getSerializerClass(), fixed != null ? fixed : (cfield.getSerializer().getStorageType().isFixed() ? ((SupportedTypes.FixedSupportedType) cfield.getSerializer().getStorageType()).getFixedAnnotation() : null)));
                }
            }
        }

        boolean hasAutoIncrement = false;
        for (DocumentField df : fields) {
            if (df.isAutoIncrement()) {
                if (hasAutoIncrement) throw new InvalidDocumentClassException("@AutoIncrement should be used only on one field");
                hasAutoIncrement = true;
            }
            if (df instanceof ComputedDocumentField) {
                ComputedDocumentField field = (ComputedDocumentField) df;
                if (field.isAutoIncrement() && !field.isPrimaryKey()) throw new InvalidDocumentClassException("@AutoIncrement used without @PrimaryKey at field " + field.getName());
                if (field.getGetter() == null || field.getSetter() == null) throw new InvalidDocumentClassException(
                        (field.getGetter() == null ? "Setter" : "Getter") + " at field " + field.getName() + " does not have matching " + (field.getSetter() == null ? "setter" : "getter")
                );
            }
        }

        DocumentSchema<T> schema = new DocumentSchema<>(clazz, tableName, fields);
        if (cacheEnabled)
            cache.add(new CachedDocumentSchema<>(clazz, tableNameModifier, fieldNameModifier, schema));
        return schema;
    }

    private static DocumentField findField(List<DocumentField> list, String name) {
        for (DocumentField f : list)
            if (Objects.equals(f.getName(), name)) return f;
        return null;
    }

    public static abstract class DocumentField {
        @Getter private final String name;
        @Getter private final boolean isPrimaryKey;
        @Getter private final boolean isAutoIncrement;
        @Getter @Setter private SerializerMiddleware serializer;

        protected DocumentField(String name, SerializerMiddleware serializer, boolean isPrimaryKey, boolean isAutoIncrement) {
            this.name = name;
            this.serializer = serializer;
            this.isPrimaryKey = isPrimaryKey;
            this.isAutoIncrement = isAutoIncrement;
        }

        public abstract Object read(Object instance) throws IllegalAccessException, InvocationTargetException;
        public abstract void write(Object instance, Object value) throws IllegalAccessException, InvocationTargetException;
    }

    public static class StandardDocumentField extends DocumentField {
        @Getter private final Field reflectField;

        private StandardDocumentField(String name, SerializerMiddleware serializer, boolean isPrimaryKey, boolean isAutoIncrement, Field reflectField) {
            super(name, serializer, isPrimaryKey, isAutoIncrement);
            this.reflectField = reflectField;
        }

        @Override
        public Object read(Object instance) throws IllegalAccessException {
            return reflectField.get(instance);
        }

        @Override
        public void write(Object instance, Object value) throws IllegalAccessException {
            reflectField.set(instance, value);
        }
    }

    public static class ComputedDocumentField extends DocumentField {
        @Getter @Setter private Method getter;
        @Getter @Setter private Method setter;

        private ComputedDocumentField(String name, SerializerMiddleware serializer, boolean isPrimaryKey, boolean isAutoIncrement) {
            super(name, serializer, isPrimaryKey, isAutoIncrement);
        }

        @Override
        public Object read(Object instance) throws IllegalAccessException, InvocationTargetException {
            return getter.invoke(instance);
        }

        @Override
        public void write(Object instance, Object value) throws IllegalAccessException, InvocationTargetException {
            setter.invoke(instance, value);
        }
    }

    @FunctionalInterface
    public interface NameModifier {
        String method(String name, Class<?> type);
    }

    @AllArgsConstructor
    private static class CachedDocumentSchema<T> {
        private final Class<?> type;
        private final DocumentParser.NameModifier tableNameModifier;
        private final DocumentParser.NameModifier fieldNameModifier;
        @Getter private final DocumentSchema<T> schema;

        public boolean equals(Class<?> type, DocumentParser.NameModifier tableNameModifier, DocumentParser.NameModifier fieldNameModifier) {
            return this.type == type && this.tableNameModifier == tableNameModifier && this.fieldNameModifier == fieldNameModifier;
        }
    }
}
