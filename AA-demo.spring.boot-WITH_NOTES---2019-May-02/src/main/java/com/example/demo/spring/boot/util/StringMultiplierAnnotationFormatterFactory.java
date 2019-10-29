package com.example.demo.spring.boot.util;

import java.text.ParseException;
import java.util.Locale;
import java.util.Set;

import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

@Component
public class StringMultiplierAnnotationFormatterFactory implements AnnotationFormatterFactory<StringMultiplier> {

    @Override
    public Set<Class<?>> getFieldTypes() {
        return Sets.newHashSet(String.class);
    }

    @Override
    public Printer<?> getPrinter(StringMultiplier annotation, Class<?> fieldType) {
        return new Printer<String>() {
            @Override
            public String print(String object, Locale locale) {
                return object;
            }
        };
    }

    @Override
    public Parser<?> getParser(StringMultiplier annotation, Class<?> fieldType) {
        return new Parser<String>() {
            @Override
            public String parse(String text, Locale locale) throws ParseException {
                StringBuilder stbl = new StringBuilder(text);
                for (int i = 0; i < Integer.valueOf(annotation.value()) - 1; i++) {
                    stbl.append(text.toUpperCase());
                }
                return stbl.toString();
            }
        };
    }

}
