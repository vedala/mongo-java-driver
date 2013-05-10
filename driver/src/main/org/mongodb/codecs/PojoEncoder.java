package org.mongodb.codecs;

import org.bson.BSONWriter;
import org.mongodb.Encoder;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class PojoEncoder<T> implements Encoder<T> {
    private final Codecs codecs;

    //at this time, this seems to be the only way to
    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, ClassModel> fieldsForClass = new HashMap<Class<?>, ClassModel>();

    public PojoEncoder(final Codecs codecs) {
        this.codecs = codecs;
    }

    @Override
    public void encode(final BSONWriter bsonWriter, final T value) {
        bsonWriter.writeStartDocument();
        encodePojo(bsonWriter, value);
        bsonWriter.writeEndDocument();
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) //bah.  maybe this isn't even correct
    private void encodePojo(final BSONWriter bsonWriter, final T value) {
        ClassModel<T> classModel = fieldsForClass.get(value.getClass());
        if (classModel == null) {
            classModel = new ClassModel(value.getClass());
            fieldsForClass.put(value.getClass(), classModel);
        }
        for (final Field field : classModel.getFields()) {
            encodeField(bsonWriter, value, field, field.getName());
        }
    }

    // need to cast the field
    @SuppressWarnings("unchecked")
    private void encodeField(final BSONWriter bsonWriter, final T value, final Field field, final String fieldName) {
        try {
            field.setAccessible(true);
            final T fieldValue = (T) field.get(value);
            bsonWriter.writeName(fieldName);
            encodeValue(bsonWriter, fieldValue);
            field.setAccessible(false);
        } catch (IllegalAccessException e) {
            //TODO: this is really going to bugger up the writer if it throws an exception halfway through writing
            throw new EncodingException("Could not encode field '" + fieldName + "' from " + value, e);
        }
    }

    private void encodeValue(final BSONWriter bsonWriter, final T fieldValue) {
        if (codecs.canEncode(fieldValue)) {
            codecs.encode(bsonWriter, fieldValue);
        } else {
            encode(bsonWriter, fieldValue);
        }
    }

    @Override
    public Class<T> getEncoderClass() {
        throw new UnsupportedOperationException("Not implemented yet!");
    }
}
