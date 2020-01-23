package com.example;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;
import java.io.IOException;
import jsm.NonBlockingParser;
import jsm.ObjectParserState;
import jsm.ParserUtils;

public class Item {

    public final String property1;
    public final com.example.ItemProperty2 property2;
    public final java.math.BigDecimal decimalProperty;

    public Item(String property1, com.example.ItemProperty2 property2, java.math.BigDecimal decimalProperty) {
        this.property1 = property1;
        this.property2 = property2;
        this.decimalProperty = decimalProperty;
    }

    public static class Parser implements NonBlockingParser<com.example.Item> {

        private ObjectParserState objectParserState = ObjectParserState.PARSE_START_OBJECT;
        private String currentField;
        private String p0; // property1
        private com.example.ItemProperty2 p1; // property2
        private java.math.BigDecimal p2; // decimalProperty
        private com.example.ItemProperty2.Parser comExampleItemProperty2Parser;

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
                                    switch (currentField) {
                                        case "property2":
                                            comExampleItemProperty2Parser = new com.example.ItemProperty2.Parser();
                                            break;
                                    }
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
                            case "property1":
                                if ((token = jsonParser.nextToken()) != JsonToken.NOT_AVAILABLE) {
                                    ParserUtils.assertToken(JsonToken.VALUE_STRING, token, jsonParser);
                                    this.p0 = jsonParser.getText();
                                    objectParserState = ObjectParserState.PARSE_FIELD_NAME_OR_END_OBJECT;
                                }
                                break;
                            case "property2":
                                boolean done = comExampleItemProperty2Parser.parseNext(jsonParser);
                                if (done) {
                                    p1 = comExampleItemProperty2Parser.build();
                                    comExampleItemProperty2Parser = null;
                                    objectParserState = ObjectParserState.PARSE_FIELD_NAME_OR_END_OBJECT;
                                }
                                break;
                            case "decimalProperty":
                                if ((token = jsonParser.nextToken()) != JsonToken.NOT_AVAILABLE) {
                                    ParserUtils.assertToken(JsonToken.VALUE_STRING, token, jsonParser);
                                    this.p2 = jsonParser.getDecimalValue();
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
        public com.example.Item build() {
            if (objectParserState == ObjectParserState.FINISHED) {
                return new com.example.Item(this.p0, this.p1, this.p2);
            } else {
                throw new IllegalStateException("Parsing is not completed");
            }
        }

    }

    public static class Writer implements jsm.Writer<com.example.Item> {
        public static final Writer INSTANCE = new Writer();

        @Override
        public void write(JsonGenerator jsonGenerator, com.example.Item value) throws IOException {
            jsonGenerator.writeStartObject();
            if (value.property1 != null) {
                jsonGenerator.writeFieldName("property1");
                jsonGenerator.writeString(value.property1);
            }
            if (value.property2 != null) {
                jsonGenerator.writeFieldName("property2");
                com.example.ItemProperty2.Writer.INSTANCE.write(jsonGenerator, value.property2);
            }
            if (value.decimalProperty != null) {
                jsonGenerator.writeFieldName("decimalProperty");
                jsonGenerator.writeNumber(value.decimalProperty);
            }
            jsonGenerator.writeEndObject();
        }
    }
}
