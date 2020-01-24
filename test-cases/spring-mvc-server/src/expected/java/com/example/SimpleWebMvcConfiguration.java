package com.example;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import jsm.ParseResult;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
public class SimpleWebMvcConfiguration extends WebMvcConfigurationSupport {

    private final JsonFactory jsonFactory;

    public SimpleWebMvcConfiguration(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }

    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        HttpMessageConverter<com.example.Item> responseConverter0 = new HttpMessageConverter<com.example.Item>() {
            @Override
            public boolean canRead(Class<?> clazz, MediaType mediaType) {
                return false;
            }

            @Override
            public boolean canWrite(Class<?> clazz, MediaType mediaType) {
                return com.example.Item.class == clazz;
            }

            @Override
            public List<MediaType> getSupportedMediaTypes() {
                return Collections.singletonList(MediaType.APPLICATION_JSON);
            }

            @Override
            public com.example.Item read(Class<? extends com.example.Item> clazz, HttpInputMessage inputMessage) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void write(com.example.Item obj, MediaType contentType, HttpOutputMessage outputMessage) throws IOException {
                OutputStream outputStream = outputMessage.getBody();
                try (JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputStream)) {
                    com.example.Item.Writer.INSTANCE.write(jsonGenerator, obj);
                }
            }

        };
        converters.add(responseConverter0);

        HttpMessageConverter<com.example.Item> requestConverter0 = new HttpMessageConverter<com.example.Item>() {
            @Override
            public boolean canRead(Class<?> clazz, MediaType mediaType) {
                return com.example.Item.class == clazz;
            }

            @Override
            public boolean canWrite(Class<?> clazz, MediaType mediaType) {
                return false;
            }

            @Override
            public List<MediaType> getSupportedMediaTypes() {
                return Collections.singletonList(MediaType.APPLICATION_JSON);
            }

            @Override
            public com.example.Item read(Class<? extends com.example.Item> clazz, HttpInputMessage inputMessage) throws IOException {
                NonBlockingJsonParser jsonParser = (NonBlockingJsonParser) jsonFactory.createNonBlockingByteArrayParser();
                InputStream inputStream = inputMessage.getBody();
                com.example.Item.Parser parser = new com.example.Item.Parser();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    jsonParser.feedInput(buffer, 0, read);
                    parser.parseNext(jsonParser);
                }
                ParseResult<Item> parseResult = parser.build();
                if (parseResult == ParseResult.NULL_VALUE) {
                    return null;
                } else {
                    return parseResult.getValue();
                }
            }

            @Override
            public void write(com.example.Item obj, MediaType contentType, HttpOutputMessage outputMessage) {
                throw new UnsupportedOperationException();
            }

        };
        converters.add(requestConverter0);
    }

}
