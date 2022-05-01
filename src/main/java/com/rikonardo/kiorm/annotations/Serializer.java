package com.rikonardo.kiorm.annotations;

import com.rikonardo.kiorm.serialization.FieldSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface Serializer {
    Class<? extends FieldSerializer<?, ?>> value();
}
