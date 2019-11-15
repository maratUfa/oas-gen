package jsm;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public class WriterUtils {
    public static <T> ByteBuf toByteBuf(Writer<T> writer, T t) {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
        OutputStream byteBufOutputStream = new ByteBufOutputStream(byteBuf);
        JsonFactory jsonFactory = new JsonFactory();
        try (JsonGenerator jsonGenerator = jsonFactory.createGenerator(byteBufOutputStream)) {
            writer.write(jsonGenerator, t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return byteBuf;
    }
}
