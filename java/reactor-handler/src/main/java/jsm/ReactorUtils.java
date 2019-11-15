package jsm;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class ReactorUtils {
    private ReactorUtils() {
    }

    public static <T> Mono<T> decode(Flux<byte[]> byteFlux, NonBlockingParser<T> parser) {
        JsonFactory jsonFactory = new JsonFactory();
        NonBlockingJsonParser jsonParser;
        try {
            jsonParser = (NonBlockingJsonParser) jsonFactory.createNonBlockingByteArrayParser();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Flux<T> valueFlux = byteFlux.handle((bytes, synchronousSink) -> {
            if (jsonParser.needMoreInput()) {
                try {
                    jsonParser.feedInput(bytes, 0, bytes.length);
                } catch (IOException e) {
                    synchronousSink.error(e);
                    return;
                }
            }

            boolean done;
            try {
                done = parser.parseNext(jsonParser);
            } catch (Exception e) {
                synchronousSink.error(e);
                return;
            }

            if (done) {
                T value = parser.build();
                synchronousSink.next(value);
            }
        });
        return valueFlux.single();
    }
}
