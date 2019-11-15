package com.example;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;
import java.io.IOException;
import jsm.NonBlockingParser;
import jsm.ObjectParserState;
import jsm.ParserUtils;

public class ItemProperty2 {

    public final String property21;
    public final String property22;

    public ItemProperty2(String property21, String property22) {
        this.property21 = property21;
        this.property22 = property22;
    }

    public static class Parser implements NonBlockingParser<com.example.ItemProperty2> {

        private ObjectParserState objectParserState = ObjectParserState.PARSE_START_OBJECT;
        private String currentField;
        private String p0; // property21
        private String p1; // property22


        @Override
        public boolean parseNext(NonBlockingJsonParser jsonParser) throws IOException {
            while (jsonParser.currentToken() == null || jsonParser.currentToken() != JsonToken.NOT_AVAILABLE) {
                JsonToken token;
                switch (objectParserState) {
                    case PARSE_START_OBJECT:
                        if ((token = jsonParser.nextToken()) != JsonToken.NOT_AVAILABLE) {
                            ParserUtils.assertToken(JsonToken.START_OBJECT, token, jsonParser);
                            objectParserState = ObjectParserState.PARSE_FIELD_NAME_OR_END_OBJECT;
                        }
                        break;
                    case PARSE_FIELD_NAME_OR_END_OBJECT:
                        if ((token = jsonParser.nextToken()) != JsonToken.NOT_AVAILABLE) {
                            switch (token) {
                                case FIELD_NAME:
                                    currentField = jsonParser.getCurrentName();
                                    objectParserState = ObjectParserState.PARSE_FIELD_VALUE;

                                    break;
                                case END_OBJECT:
                                    objectParserState = ObjectParserState.FINISHED;
                                    return true;
                                default:
                                    throw new RuntimeException("Unexpected token " + token);
                            }
                        }
                        break;
                    case PARSE_FIELD_VALUE:
                        switch (currentField) {
                            case "property21":
                                if ((token = jsonParser.nextToken()) != JsonToken.NOT_AVAILABLE) {
                                    ParserUtils.assertToken(JsonToken.VALUE_STRING, token, jsonParser);
                                    this.p0 = jsonParser.getValueAsString();
                                    objectParserState = ObjectParserState.PARSE_FIELD_NAME_OR_END_OBJECT;
                                }
                                break;
                            case "property22":
                                if ((token = jsonParser.nextToken()) != JsonToken.NOT_AVAILABLE) {
                                    ParserUtils.assertToken(JsonToken.VALUE_STRING, token, jsonParser);
                                    this.p1 = jsonParser.getValueAsString();
                                    objectParserState = ObjectParserState.PARSE_FIELD_NAME_OR_END_OBJECT;
                                }
                                break;
                        }
                        break;
                }
            }
            return false;
        }

        @Override
        public com.example.ItemProperty2 build() {
            if (objectParserState == ObjectParserState.FINISHED) {
                return new com.example.ItemProperty2(this.p0, this.p1);
            } else {
                throw new IllegalStateException("Parsing is not completed");
            }
        }

    }

    public static class Writer implements jsm.Writer<com.example.ItemProperty2> {
        public static final Writer INSTANCE = new Writer();

        @Override
        public void write(JsonGenerator jsonGenerator, com.example.ItemProperty2 value) throws IOException {
            jsonGenerator.writeStartObject();
            if (value.property21 != null) {
                jsonGenerator.writeFieldName("property21");
                jsonGenerator.writeString(value.property21);
            }
            if (value.property22 != null) {
                jsonGenerator.writeFieldName("property22");
                jsonGenerator.writeString(value.property22);
            }
            jsonGenerator.writeEndObject();
        }
    }
}
