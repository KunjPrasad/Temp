package com.example.demo.spring.boot.dto;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.example.demo.spring.boot.exception.ExceptionLogLevel;
import com.example.demo.spring.boot.exception.Response400Exception;
import com.example.demo.spring.boot.util.ApplicationConstants;
import com.example.demo.spring.boot.util.CsvMediaType;

/**
 * This class contains logic to serialize and deserialize csv data for TestDTO class
 * 
 * @author KunjPrasad
 *
 */
public class TestDtoCsvMessageConverter implements HttpMessageConverter<TestDTO> {

    private MediaType csvMediaType;

    public TestDtoCsvMessageConverter() {
        this.csvMediaType = new CsvMediaType();
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return TestDTO.class.isAssignableFrom(clazz)
                && csvMediaType.isCompatibleWith(mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return TestDTO.class.isAssignableFrom(clazz)
                // NOTE: always use csvMediaType on LHS of comparison because Spring sometimes seems to give null for
                // mediaType
                && csvMediaType.isCompatibleWith(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(csvMediaType);
    }

    @Override
    public TestDTO read(Class<? extends TestDTO> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        String csvBody = IOUtils.toString(inputMessage.getBody(), ApplicationConstants.UTF8_CHARSET);
        if (csvBody == null || csvBody.split(",").length != 1) {
            throw new Response400Exception(null, "Csv request in improper format",
                    "A non-null, size-1 entry expected for TestDTO request as csv. provided=" + csvBody,
                    ExceptionLogLevel.INFO);
        }
        TestDTO testDTO = new TestDTO();
        testDTO.setMessage(csvBody);
        return testDTO;
    }

    @Override
    public void write(TestDTO t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        IOUtils.write(t.getMessage() + ",csvOutput", outputMessage.getBody(), ApplicationConstants.UTF8_CHARSET);
    }
}
