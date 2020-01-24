package jsm;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ObjectArrayParser<T> implements NonBlockingParser<List<T>> {

    private ArrayParserState arrayParserState = ArrayParserState.PARSE_START_ARRAY_OR_NULL_VALUE_OR_END_ARRAY;
    private final List<T> items = new ArrayList<>();
    private final NonBlockingParser<T> itemParser;

    public ObjectArrayParser(NonBlockingParser<T> itemParser) {
        this.itemParser = itemParser;
    }

    @Override
    public boolean parseNext(NonBlockingJsonParser jsonParser) throws IOException {
        while (jsonParser.currentToken() == null || jsonParser.currentToken() != JsonToken.NOT_AVAILABLE) {
            JsonToken token;
            switch (arrayParserState) {
                case PARSE_START_ARRAY_OR_NULL_VALUE_OR_END_ARRAY:
                    if ((token = jsonParser.nextToken()) != JsonToken.NOT_AVAILABLE) {
                        switch (token) {
                            case START_ARRAY:
                                arrayParserState = ArrayParserState.PARSE_VALUE;
                                break;
                            case VALUE_NULL:
                                arrayParserState = ArrayParserState.FINISHED_NULL;
                                return true;
                            case END_ARRAY:
                                arrayParserState = ArrayParserState.FINISHED_ARRAY;
                                return true;
                            default:
                                throw new RuntimeException("Unexpected token " + token);
                        }
                    }
                    break;
                case PARSE_VALUE:
                    boolean itemCompleted = itemParser.parseNext(jsonParser);
                    if (itemCompleted) {
                        ParseResult<T> parseResult = itemParser.build();
                        if (parseResult == ParseResult.NULL_VALUE) {
                            items.add(null);
                        } else if (parseResult == ParseResult.END_ARRAY) {
                            arrayParserState = ArrayParserState.FINISHED_ARRAY;
                            return true;
                        } else {
                            T value = parseResult.getValue();
                            items.add(value);
                        }
                        itemParser.reset();
                    }
            }
        }
        return false;
    }

    @Override
    public ParseResult<List<T>> build() {
        switch (arrayParserState) {
            case FINISHED_LIST:
                return new ParseResult.Value<>(items);
            case FINISHED_ARRAY:
                return ParseResult.endArray();
            case FINISHED_NULL:
                return ParseResult.nullValue();
            default:
                throw new IllegalStateException("Parsing is not completed");
        }
    }

    @Override
    public void reset() {
        items.clear();
    }
}
