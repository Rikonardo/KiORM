package com.rikonardo.kiorm.serialization;

public interface FieldSerializer<R, S> {
    S serialize(R value);
    R deserialize(S value);
}
