package jsm.java.spring.mvc

import jsm.OutputFile
import jsm.indentWithMargin
import jsm.java.JavaOperation
import jsm.java.getFilePath

class ConfigurationWriter {
    fun write(
            basePackage: String,
            simpleClassName: String,
            javaOperations: List<JavaOperation>
    ): OutputFile {
        val responseConverters = javaOperations.mapNotNull { javaOperation ->
            if (javaOperation.responseVariable.schema.type.scalar) null
            else javaOperation.responseVariable
        }.mapIndexed { index, responseVariable ->
            """|HttpMessageConverter<${responseVariable.type}> responseConverter$index = new HttpMessageConverter<${responseVariable.type}>() {
               |    @Override
               |    public boolean canRead(Class<?> clazz, MediaType mediaType) {
               |        return false;
               |    }
               |
               |    @Override
               |    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
               |        return ${responseVariable.type}.class == clazz;
               |    }
               |
               |    @Override
               |    public List<MediaType> getSupportedMediaTypes() {
               |        return Collections.singletonList(MediaType.APPLICATION_JSON);
               |    }
               |
               |    @Override
               |    public ${responseVariable.type} read(Class<? extends ${responseVariable.type}> clazz, HttpInputMessage inputMessage) {
               |        throw new UnsupportedOperationException();
               |    }
               |
               |    @Override
               |    public void write(${responseVariable.type} obj, MediaType contentType, HttpOutputMessage outputMessage) throws IOException {
               |        OutputStream outputStream = outputMessage.getBody();
               |        try (JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputStream)) {
               |            ${responseVariable.type}.Writer.INSTANCE.write(jsonGenerator, obj);
               |        }
               |    }
               |
               |};
               |converters.add(responseConverter$index);
               |
            """.trimMargin()
        }

        val requestConverters = javaOperations.mapNotNull { javaOperation ->
            if (javaOperation.requestVariable != null && !javaOperation.requestVariable.schema.type.scalar)
                javaOperation.requestVariable
            else
                null
        }.mapIndexed { index, requestVariable ->
            """|HttpMessageConverter<${requestVariable.type}> requestConverter$index = new HttpMessageConverter<${requestVariable.type}>() {
               |    @Override
               |    public boolean canRead(Class<?> clazz, MediaType mediaType) {
               |        return ${requestVariable.type}.class == clazz;
               |    }
               |
               |    @Override
               |    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
               |        return false;
               |    }
               |
               |    @Override
               |    public List<MediaType> getSupportedMediaTypes() {
               |        return Collections.singletonList(MediaType.APPLICATION_JSON);
               |    }
               |
               |    @Override
               |    public ${requestVariable.type} read(Class<? extends ${requestVariable.type}> clazz, HttpInputMessage inputMessage) throws IOException {
               |        NonBlockingJsonParser jsonParser = (NonBlockingJsonParser) jsonFactory.createNonBlockingByteArrayParser();
               |        InputStream inputStream = inputMessage.getBody();
               |        ${requestVariable.type}.Parser parser = new ${requestVariable.type}.Parser();
               |        byte[] buffer = new byte[8192];
               |        int read;
               |        while ((read = inputStream.read(buffer)) >= 0) {
               |            jsonParser.feedInput(buffer, 0, read);
               |            parser.parseNext(jsonParser);
               |        }
               |        return parser.build();
               |    }
               |
               |    @Override
               |    public void write(${requestVariable.type} obj, MediaType contentType, HttpOutputMessage outputMessage) {
               |        throw new UnsupportedOperationException();
               |    }
               |
               |};
               |converters.add(requestConverter$index);
               |
            """.trimMargin()
        }


        val content = """
               |package $basePackage;
               |
               |import com.fasterxml.jackson.core.JsonFactory;
               |import com.fasterxml.jackson.core.JsonGenerator;
               |import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;
               |import java.io.IOException;
               |import java.io.InputStream;
               |import java.io.OutputStream;
               |import java.util.Collections;
               |import java.util.List;
               |import org.springframework.context.annotation.Configuration;
               |import org.springframework.http.HttpInputMessage;
               |import org.springframework.http.HttpOutputMessage;
               |import org.springframework.http.MediaType;
               |import org.springframework.http.converter.HttpMessageConverter;
               |import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
               |
               |@Configuration
               |public class $simpleClassName extends WebMvcConfigurationSupport {
               |
               |    private final JsonFactory jsonFactory;
               |
               |    public SimpleWebMvcConfiguration(JsonFactory jsonFactory) {
               |        this.jsonFactory = jsonFactory;
               |    }
               |
               |    @Override
               |    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
               |        ${responseConverters.indentWithMargin(2)}
               |
               |        ${requestConverters.indentWithMargin(2)}
               |    }
               |
               |}
               |""".trimMargin()
        return OutputFile(getFilePath("$basePackage.$simpleClassName"), content)
    }
}