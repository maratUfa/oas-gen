package jsm.java.jackson

import jsm.JsonType
import jsm.indentWithMargin
import jsm.java.ExtraMemberWriter
import jsm.java.JavaProperty
import jsm.java.toVariableName
import java.util.*

val jacksonScalarReadMethods = mapOf(
        JsonType.STRING to "getValueAsString"
)

class JacksonParserWriter : ExtraMemberWriter {
    override fun write(
            importDeclarations: MutableSet<String>,
            javaProperties: List<JavaProperty>,
            dtoClassName: String
    ): String {
        importDeclarations.addAll(listOf(
                "import com.fasterxml.jackson.core.JsonGenerator;",
                "import com.fasterxml.jackson.core.JsonToken;",
                "import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;",
                "import java.io.IOException;",
                "import jsm.NonBlockingParser;",
                "import jsm.ObjectParserState;",
                "import jsm.ParserUtils;"
        ))

        val builderPropertyDeclarations = javaProperties.map { javaProperty ->
            "private ${javaProperty.type} ${javaProperty.internalVariableName}; // ${javaProperty.name}"
        }

        val nonScalarProperties = javaProperties.filter { !it.jsonSchema.type.scalar }

        val propertyParserDeclarations = TreeSet(nonScalarProperties.map { javaProperty ->
            "private ${javaProperty.type}.Parser ${toVariableName(javaProperty.type, "parser")};"
        })

        val initPropertyParsers = if (nonScalarProperties.isNotEmpty()) {
            val cases = nonScalarProperties.map { javaProperty ->
                """|case "${javaProperty.name}":
                   |    ${toVariableName(javaProperty.type, "parser")} = new ${javaProperty.type}.Parser();
                   |    break;
                """.trimMargin()
            }
            """
               |switch (currentField) {
               |    ${cases.indentWithMargin(1)}
               |}
            """.trimMargin()
        } else {
            ""
        }

        val parseValueCases = javaProperties.map { javaProperty ->
            val jsonType = javaProperty.jsonSchema.type
            if (jsonType.scalar) {
                val jacksonValueMethod = jacksonScalarReadMethods[jsonType]
                        ?: error("Can't find value method for $jsonType")
                """|case "${javaProperty.name}":
                   |    if ((token = jsonParser.nextToken()) != JsonToken.NOT_AVAILABLE) {
                   |        ParserUtils.assertToken(JsonToken.VALUE_STRING, token, jsonParser);
                   |        this.${javaProperty.internalVariableName} = jsonParser.$jacksonValueMethod();
                   |        objectParserState = ObjectParserState.PARSE_FIELD_NAME_OR_END_OBJECT;
                   |    }
                   |    break;
                """.trimMargin()
            } else {
                """|case "${javaProperty.name}":
                   |    boolean done = ${toVariableName(javaProperty.type, "parser")}.parseNext(jsonParser);
                   |    if (done) {
                   |        ${javaProperty.internalVariableName} = ${toVariableName(javaProperty.type, "parser")}.build();
                   |        ${toVariableName(javaProperty.type, "parser")} = null;
                   |        objectParserState = ObjectParserState.PARSE_FIELD_NAME_OR_END_OBJECT;
                   |    }
                   |    break;
                """.trimMargin()
            }
        }

        val constructorArgs = javaProperties.joinToString(", ") { "this.${it.internalVariableName}" }

        return """
               |public static class Parser implements NonBlockingParser<$dtoClassName> {
               |
               |    private ObjectParserState objectParserState = ObjectParserState.PARSE_START_OBJECT;
               |    private String currentField;
               |    ${builderPropertyDeclarations.indentWithMargin(1)}
               |    ${propertyParserDeclarations.indentWithMargin(1)}
               |
               |    @Override
               |    public boolean parseNext(NonBlockingJsonParser jsonParser) throws IOException {
               |        while (jsonParser.currentToken() == null || jsonParser.currentToken() != JsonToken.NOT_AVAILABLE) {
               |            JsonToken token;
               |            switch (objectParserState) {
               |                case PARSE_START_OBJECT:
               |                    if ((token = jsonParser.nextToken()) != JsonToken.NOT_AVAILABLE) {
               |                        ParserUtils.assertToken(JsonToken.START_OBJECT, token, jsonParser);
               |                        objectParserState = ObjectParserState.PARSE_FIELD_NAME_OR_END_OBJECT;
               |                    }
               |                    break;
               |                case PARSE_FIELD_NAME_OR_END_OBJECT:
               |                    if ((token = jsonParser.nextToken()) != JsonToken.NOT_AVAILABLE) {
               |                        switch (token) {
               |                            case FIELD_NAME:
               |                                currentField = jsonParser.getCurrentName();
               |                                objectParserState = ObjectParserState.PARSE_FIELD_VALUE;
               |                                ${initPropertyParsers.indentWithMargin(8)}
               |                                break;
               |                            case END_OBJECT:
               |                                objectParserState = ObjectParserState.FINISHED;
               |                                return true;
               |                            default:
               |                                throw new RuntimeException("Unexpected token " + token);
               |                        }
               |                    }
               |                    break;
               |                case PARSE_FIELD_VALUE:
               |                    switch (currentField) {
               |                        ${parseValueCases.indentWithMargin(6)}
               |                    }
               |                    break;
               |            }
               |        }
               |        return false;
               |    }
               |
               |    @Override
               |    public $dtoClassName build() {
               |        if (objectParserState == ObjectParserState.FINISHED) {
               |            return new $dtoClassName($constructorArgs);
               |        } else {
               |            throw new IllegalStateException("Parsing is not completed");
               |        }
               |    }
               |
               |}
               |
            """.trimMargin()
    }
}