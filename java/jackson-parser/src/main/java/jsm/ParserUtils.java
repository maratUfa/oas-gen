package jsm;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public final class ParserUtils {

    private ParserUtils() {
    }

    public static void assertToken(JsonToken expectedToken, JsonToken token, JsonParser jsonParser) {
        if (token != expectedToken) {
            throw new RuntimeException("expected " + expectedToken + " but was " + token + " at " + jsonParser.getCurrentLocation());
        }
    }

}
