package com.rikonardo.kiorm.serialization;

import com.rikonardo.kiorm.annotations.Fixed;
import lombok.Getter;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SupportedTypes {
    public static class Registry {
        public static final SupportedType STRING = new SupportedType(false, String.class, "TEXT", "STRING",
                (s, i) -> (i instanceof String ? s.getString((String) i) : s.getString((int) i)),
                (s, i, v) -> s.setString(i, (String) v)
        );
        public static final SupportedType BOOLEAN = new SupportedType(false, Boolean.class, boolean.class, "BOOLEAN", "BOOLEAN",
                (s, i) -> (i instanceof String ? s.getBoolean((String) i) : s.getBoolean((int) i)),
                (s, i, v) -> s.setBoolean(i, (boolean) v)
        );
        public static final SupportedType BYTE = new SupportedType(false, Byte.class, byte.class, "TINYINT", "BYTE",
                (s, i) -> (i instanceof String ? s.getByte((String) i) : s.getByte((int) i)),
                (s, i, v) -> s.setByte(i, (byte) v)
        );
        public static final SupportedType SHORT = new SupportedType(false, Short.class, short.class, "SMALLINT", "SHORT",
                (s, i) -> (i instanceof String ? s.getShort((String) i) : s.getShort((int) i)),
                (s, i, v) -> s.setShort(i, (short) v)
        );
        public static final SupportedType INT = new SupportedType(false, Integer.class, int.class, "INT", "INT",
                (s, i) -> (i instanceof String ? s.getInt((String) i) : s.getInt((int) i)),
                (s, i, v) -> s.setInt(i, (int) v)
        );
        public static final SupportedType LONG = new SupportedType(false, Long.class, long.class, "BIGINT", "LONG",
                (s, i) -> (i instanceof String ? s.getLong((String) i) : s.getLong((int) i)),
                (s, i, v) -> s.setLong(i, (long) v)
        );
        public static final SupportedType FLOAT = new SupportedType(false, Float.class, float.class, "FLOAT", "FLOAT",
                (s, i) -> (i instanceof String ? s.getFloat((String) i) : s.getFloat((int) i)),
                (s, i, v) -> s.setFloat(i, (float) v)
        );
        public static final SupportedType DOUBLE = new SupportedType(false, Double.class, double.class, "DOUBLE", "DOUBLE",
                (s, i) -> (i instanceof String ? s.getDouble((String) i) : s.getDouble((int) i)),
                (s, i, v) -> s.setDouble(i, (double) v)
        );
        public static final SupportedType BINARY = new SupportedType(false, byte[].class, "BLOB", "BINARY",
                (s, i) -> (i instanceof String ? s.getBytes((String) i) : s.getBytes((int) i)),
                (s, i, v) -> s.setBytes(i, (byte[]) v)
        );

        public static class Fixed {
            public static final SupportedType BINARY = new SupportedType(true, byte[].class, "BINARY", "FIXED_BINARY",
                    (s, i) -> (i instanceof String ? s.getBytes((String) i) : s.getBytes((int) i)),
                    (s, i, v) -> s.setBytes(i, (byte[]) v)
            );
            public static final SupportedType STRING = new SupportedType(true, String.class, "CHAR", "STRING",
                    (s, i) -> (i instanceof String ? s.getString((String) i) : s.getString((int) i)),
                    (s, i, v) -> s.setString(i, (String) v)
            );
        }
    }

    public static boolean checkPrimitiveObjectTypesPair(Class<?> a, Class<?> b) {
        for (SupportedType t : FIELD_TYPES) {
            if (
                t.getPrimitiveType() == a && t.getType() == b ||
                t.getPrimitiveType() == b && t.getType() == a
            ) return true;
        }
        return false;
    }

    public static SupportedType[] FIELD_TYPES = {
            Registry.STRING,
            Registry.BOOLEAN,
            Registry.BYTE,
            Registry.SHORT,
            Registry.INT,
            Registry.LONG,
            Registry.FLOAT,
            Registry.DOUBLE,
            Registry.BINARY,
            Registry.Fixed.BINARY,
            Registry.Fixed.STRING,
    };

    public static SupportedType getFieldType(Class<?> type, Fixed fixed) {
        if (fixed == null) {
            for (SupportedType t : FIELD_TYPES)
                if ((t.getType() == type || t.getPrimitiveType() == type) && !t.isFixed())
                    return t;
        } else {
            for (SupportedType t : FIELD_TYPES)
                if ((t.getType() == type || t.getPrimitiveType() == type) && t.isFixed())
                    return t.getFixedType(fixed);
        }
        return null;
    }

    public static SupportedType getAnyFieldType(Class<?> type) {
        for (SupportedType t : FIELD_TYPES)
            if (t.getType() == type || t.getPrimitiveType() == type)
                return t;
        return null;
    }

    public static class SupportedType {
        @Getter private final Class<?> type;
        @Getter private final Class<?> primitiveType;
        @Getter private final String sqlName;
        @Getter private final String name;
        @Getter private final boolean fixed;
        private final ReadAction read;
        private final WriteAction write;

        private SupportedType(boolean fixed, Class<?> type, Class<?> primitiveType, String sqlName, String name, ReadAction read, WriteAction write) {
            this.fixed = fixed;
            this.type = type;
            this.primitiveType = primitiveType;
            this.sqlName = sqlName;
            this.name = name;
            this.read = read;
            this.write = write;
        }

        private SupportedType(boolean fixed, Class<?> type, String sqlName, String name, ReadAction read, WriteAction write) {
            this(fixed, type, null, sqlName, name, read, write);
        }

        public Object read(ResultSet resultSet, Object index) throws SQLException {
            return this.read.method(resultSet, index);
        }

        public void write(PreparedStatement preparedStatement, int index, Object value) throws SQLException {
            this.write.method(preparedStatement, index, value);
        }

        @FunctionalInterface
        interface ReadAction {
            Object method(ResultSet s, Object i) throws SQLException;
        }

        @FunctionalInterface
        interface WriteAction {
            void method(PreparedStatement s, int i, Object v) throws SQLException;
        }

        public SupportedType getFixedType(Fixed fixedAnnotation) {
            if (!this.fixed) throw new RuntimeException("Cant create fixed type from non-fixed type");
            return new FixedSupportedType(fixedAnnotation, this.type, this.primitiveType, this.sqlName + "(" + fixedAnnotation.value() + ")", this.name, this.read, this.write);
        }
    }

    public static class FixedSupportedType extends SupportedType {
        @Getter private final Fixed fixedAnnotation;

        private FixedSupportedType(Fixed fixedAnnotation, Class<?> type, Class<?> primitiveType, String sqlName, String name, ReadAction read, WriteAction write) {
            super(true, type, primitiveType, sqlName, name, read, write);
            this.fixedAnnotation = fixedAnnotation;
        }

        private FixedSupportedType(Fixed fixedAnnotation, Class<?> type, String sqlName, String name, ReadAction read, WriteAction write) {
            super(true, type, sqlName, name, read, write);
            this.fixedAnnotation = fixedAnnotation;
        }
    }
}
