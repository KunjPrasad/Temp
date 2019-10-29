package com.example.demo.spring.boot.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Custom Json Serializer class
 * 
 * @author KunjPrasad
 *
 */
public class StringUpperSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String arg0, JsonGenerator arg1, SerializerProvider arg2) throws IOException {
        arg1.writeString(arg0 == null ? null : arg0.toUpperCase());

    }

}
