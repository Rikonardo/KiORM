package com.rikonardo.kiorm.serialization;

import com.rikonardo.kiorm.annotations.Fixed;
import com.rikonardo.kiorm.annotations.Serializer;
import com.rikonardo.kiorm.exceptions.InvalidDocumentClassException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SerializerMiddleware {
    private final Class<?> realType;
    private final Class<?> storageType;
    private final SupportedTypes.SupportedType supportedStorageType;
    private final Object serializerInstance;
    private final Class<? extends FieldSerializer<?, ?>> serializerClass;
    private final Method deserialize, serialize;

    public SerializerMiddleware(Class<?> realType, Serializer serializer, Fixed fixed) {
        this(realType, serializer == null ? null : serializer.value(), fixed);
    }

    public SerializerMiddleware(Class<?> realType, Class<? extends FieldSerializer<?, ?>> serializer, Fixed fixed) {
        this.serializerClass = serializer;
        this.realType = realType;
        if (serializer == null) {
            this.storageType = this.realType;
            this.serializerInstance = null;
            this.deserialize = this.serialize = null;
        }
        else {
            try {
                serializerInstance = serializer.getConstructor().newInstance();
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
                throw new InvalidDocumentClassException("Can't initialize serializer " + serializer.getName() + ", it must have no-args constructor");
            }
            Method deserialize = null, serialize = null;
            for (Method m : FieldSerializer.class.getDeclaredMethods()) {
                for (Method realM : serializer.getDeclaredMethods()) {
                    if (
                        m.getName().equals(realM.getName()) &&
                        realM.getParameterTypes().length == 1 &&
                        SupportedTypes.getFieldType(realM.getReturnType(), fixed) != null &&
                        (
                            realM.getParameterTypes()[0].isAssignableFrom(this.realType) ||
                            SupportedTypes.checkPrimitiveObjectTypesPair(this.realType, realM.getParameterTypes()[0])
                        )
                    ) {
                        serialize = realM;
                    }
                }
            }
            if (serialize == null)
                throw new InvalidDocumentClassException("Serializer " + serializer.getName() + " is not compatible with type " + realType.getName());
            this.storageType = serialize.getReturnType();
            for (Method m : FieldSerializer.class.getDeclaredMethods()) {
                for (Method realM : serializer.getDeclaredMethods()) {
                    if (
                        m.getName().equals(realM.getName()) &&
                        realM.getParameterTypes().length == 1 &&
                        realM.getParameterTypes()[0].equals(serialize.getReturnType()) &&
                        (
                            this.realType.isAssignableFrom(realM.getReturnType()) ||
                            SupportedTypes.checkPrimitiveObjectTypesPair(this.realType, realM.getReturnType())
                        )
                    ) deserialize = realM;
                }
            }
            if (deserialize == null)
                throw new InvalidDocumentClassException("Serializer " + serializer.getName() + " is not compatible with type " + realType.getName());
            this.deserialize = deserialize;
            this.serialize = serialize;
        }
        this.supportedStorageType = SupportedTypes.getFieldType(this.storageType, fixed);
        if (this.supportedStorageType == null) throw new InvalidDocumentClassException("Field type " + this.storageType.getName() + " is not supported");
    }

    public boolean hasSerializer() {
        return this.serializerInstance != null;
    }

    public Class<? extends FieldSerializer<?, ?>> getSerializerClass() {
        return serializerClass;
    }

    public Class<?> getRealType() {
        return this.realType;
    }

    public Class<?> getStorageTypeClass() {
        return this.storageType;
    }

    public SupportedTypes.SupportedType getStorageType() {
        return this.supportedStorageType;
    }

    public Object serialize(Object value) {
        if (this.serializerInstance == null) return value;
        try {
            return this.serialize.invoke(this.serializerInstance, value);
        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            throw new InvalidDocumentClassException("Failed to invoke serialization method in serializer " + this.serializerInstance.getClass().getName(), e);
        }
    }

    public Object deserialize(Object value) {
        if (this.serializerInstance == null) return value;
        try {
            return this.deserialize.invoke(this.serializerInstance, value);
        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            throw new InvalidDocumentClassException("Failed to invoke deserialization method in serializer " + this.serializerInstance.getClass().getName(), e);
        }
    }
}
